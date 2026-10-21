package com.eimsound.eimusic

import android.app.PendingIntent
import android.util.Log
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EIMusicMediaSessionService : MediaSessionService(), KoinComponent {
    private lateinit var mediaSession: MediaSession
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pendingSeekCallback: (Long) -> Unit = {}

    private val trackUriResolverFactory: TrackUriResolverFactory by inject()

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        Log.d("EIMusicMediaSessionService", "Service created")


        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true)
            .apply {
                val dataSourceFactory: ResolvingDataSource.Factory = ResolvingDataSource.Factory(
                    DefaultDataSource.Factory(EIMusicApplication.instance),
                ) {
                    val trackId = TrackId.parse(it.uri.toString())
                    val trackUriResolver = trackUriResolverFactory.get(trackId.source)
                    val resolveUri = runBlocking {
                        trackUriResolver.resolve(
                            Track(
                                id = trackId,
                                source = trackId.source,
                                uri = trackId.id,
                                name = "",
                                album = null,
                                artists = null,
                                duration = null,
                            )
                        )
                    }
                    val realUri = resolveUri?.toUri()?.let { uri ->
                        it.withUri(uri)
                    } ?: it
                    realUri
                }
                setMediaSourceFactory(
                    DefaultMediaSourceFactory(dataSourceFactory)
                )
            }
            .build()

        val interceptorPlayer = object : ForwardingPlayer(player) {
            override fun seekTo(positionMs: Long) {
                pendingSeekCallback.invoke(positionMs)
                pendingSeekCallback = {}
                super.seekTo(positionMs)
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true

                    Player.COMMAND_GET_TIMELINE -> true
                    else -> super.isCommandAvailable(command)
                }
            }
        }
        val sessionCallback = object : MediaSession.Callback {

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {

                if (customCommand.customAction == "ACTION_SET_SHUFFLE_SEED") {
                    val seed = args.getLong("seed")
                    val indices = args.getIntArray("indices")
                    if (indices != null) {
                        player.shuffleOrder = ShuffleOrder.DefaultShuffleOrder(indices, seed)
                    } else {
                        player.shuffleOrder = ShuffleOrder.DefaultShuffleOrder(player.mediaItemCount, seed)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                val isSystemNotification =
                    controller.connectionHints.getBoolean("androidx.media3.session.MediaNotificationManager", false)

                if (isSystemNotification) when (playerCommand) {
                    Player.COMMAND_PLAY_PAUSE -> {
                        val isPlaying = session.player.isPlaying
                        EventBus.PlaybackEventBus.send(
                            PlaybackEvent.PlayStateChanged(
                                !isPlaying,
                                source = EventSource.System
                            )
                        )
                    }

                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                        pendingSeekCallback = { position ->
                            EventBus.PlaybackEventBus.send(
                                PlaybackEvent.Seek(
                                    Duration(position),
                                    source = EventSource.System
                                )
                            )
                        }
                    }

                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_NEXT -> {
                        EventBus.PlaybackEventBus.send(PlaybackEvent.Next(source = EventSource.System))
                    }

                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS -> {
                        EventBus.PlaybackEventBus.send(PlaybackEvent.Previous(source = EventSource.System))
                    }
                }
                return SessionResult.RESULT_SUCCESS
            }

            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {

                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand("ACTION_SET_SHUFFLE_SEED", Bundle.EMPTY)) // 必须显式添加
                    .build()

                val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_GET_TIMELINE) // 获取播放列表权限
                    .add(Player.COMMAND_SEEK_TO_NEXT) // 下一首
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) // 下一个媒体项
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS) // 上一首
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) // 上一个媒体项
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .build()
            }
        }

        mediaSession = MediaSession.Builder(this, interceptorPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .build()

        addSession(mediaSession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        Log.d("EIMusicMediaSessionService", "Service destroyed")
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
        coroutineScope.cancel()
    }
}
package com.eimsound.eimusic.media

import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.music.nextTrackCandidate
import com.eimsound.eimusic.music.previousTrackCandidate
import com.eimsound.eimusic.music.withTrackAdded
import com.eimsound.eimusic.music.withTrackRemoved
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.playback.PlaybackOrigin
import com.eimsound.eimusic.playback.toEventSource
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLAudioElement

/** Browser audio element player. Resolved stream URLs can be refreshed by the resolver. */
actual class MediaPlayerController actual constructor(
    private val trackUriResolverFactory: TrackUriResolverFactory,
) : IMediaPlayerController {
    private var audio: HTMLAudioElement? = null
    private var resolveJob: Job? = null
    private var generation = 0L
    private var resolvingGeneration: Long? = null
    private var loadedGeneration = -1L
    private var loadedTrackId: TrackId? = null
    private var suppressedPauseEvents = 0
    private var pendingPosition: Long? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutableState = MutableStateFlow(PlayerState())
    private val mutableEvent = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 16)
    actual override val state: StateFlow<PlayerState> = mutableState.asStateFlow()
    actual override val event: SharedFlow<PlaybackEvent> = mutableEvent.asSharedFlow()

    private fun element(): HTMLAudioElement = audio ?:
        (document.createElement("audio") as HTMLAudioElement).also { el ->
            audio = el
            el.onplaying = {
                if (loadedGeneration == generation && loadedTrackId == mutableState.value.current?.id && !el.paused) {
                    mutableState.value = mutableState.value.copy(playState = PlayState.PLAYING, isLoading = false)
                }
                null
            }
            el.onpause = {
                if (suppressedPauseEvents > 0) suppressedPauseEvents--
                else if (loadedGeneration == generation && mutableState.value.playState == PlayState.PLAYING) {
                    mutableState.value = mutableState.value.copy(playState = PlayState.PAUSED)
                }
                null
            }
            el.onended = {
                if (loadedGeneration == generation && loadedTrackId == mutableState.value.current?.id && el.ended) {
                    next(isAudioCompleted = true, origin = PlaybackOrigin.InternalAutoAdvance)
                }
                null
            }
            el.onerror = { _, _, _, _, _ ->
                if (loadedGeneration == generation) mutableState.value = mutableState.value.copy(isLoading = false, playState = PlayState.STOPPED)
                null
            }
            el.ontimeupdate = {
                if (loadedGeneration == generation) mutableState.value = mutableState.value.copy(
                    position = (el.currentTime * 1000).toLong(),
                    duration = if (el.duration.isFinite()) (el.duration * 1000).toLong() else 0L,
                )
                null
            }
            el.onloadedmetadata = {
                if (loadedGeneration == generation) {
                    val duration = if (el.duration.isFinite()) (el.duration * 1000).toLong() else 0L
                    val seekTo = pendingPosition
                    if (seekTo != null) {
                        pendingPosition = null
                        el.currentTime = seekTo.coerceAtLeast(0).toDouble() / 1000
                    }
                    mutableState.value = mutableState.value.copy(duration = duration, position = seekTo ?: mutableState.value.position)
                }
                null
            }
        }

    actual override fun queue(medias: List<Track>, prepareOver: () -> Unit) {
        val currentId = mutableState.value.current?.id
        mutableState.value = mutableState.value.copy(queue = medias, index = medias.indexOfFirst { it.id == currentId }.coerceAtLeast(0))
        prepareOver()
    }

    actual override fun play(media: Track, origin: PlaybackOrigin) {
        playResolved(media, origin, isAudioCompleted = false)
    }

    private fun playResolved(media: Track, origin: PlaybackOrigin, isAudioCompleted: Boolean) {
        invalidatePendingPlayback()
        pauseForReplacement()
        val token = generation
        val currentQueue = mutableState.value.queue
        mutableState.value = mutableState.value.copy(
            current = media,
            index = currentQueue.indexOfFirst { it.id == media.id }.coerceAtLeast(0),
            position = 0,
            duration = media.duration?.milliseconds ?: 0L,
            isLoading = true,
            playState = PlayState.PLAYING,
        )
        pendingPosition = 0L
        mutableEvent.tryEmit(PlaybackEvent.TrackChanged(media, isAudioCompleted = isAudioCompleted, source = origin.toEventSource()))

        resolveTrack(media, token)
    }

    private fun resolveTrack(media: Track, token: Long) {
        if (resolvingGeneration == token) return
        resolvingGeneration = token
        resolveJob = scope.launch {
            try {
                val uri = trackUriResolverFactory.get(media.source).resolve(media)
                    ?: error("No playable audio URL for ${media.id}")
                if (!isCurrent(token, media)) return@launch
                val el = element()
                loadedGeneration = token
                loadedTrackId = media.id
                el.src = uri
                el.load()
                if (mutableState.value.playState == PlayState.PLAYING) {
                    el.play().catch {
                        if (isCurrent(token, media)) mutableState.value = mutableState.value.copy(isLoading = false, playState = PlayState.PAUSED)
                        null
                    }
                } else {
                    mutableState.value = mutableState.value.copy(isLoading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                if (isCurrent(token, media)) mutableState.value = mutableState.value.copy(isLoading = false, playState = PlayState.STOPPED)
            } finally {
                if (resolvingGeneration == token) resolvingGeneration = null
            }
        }
    }

    private fun isCurrent(token: Long, track: Track) = token == generation && mutableState.value.current?.id == track.id

    private fun invalidatePendingPlayback() {
        generation++
        resolveJob?.cancel()
        resolveJob = null
        resolvingGeneration = null
    }

    private fun pauseForReplacement() {
        val el = audio ?: return
        if (!el.paused) {
            suppressedPauseEvents++
            el.pause()
        }
    }

    actual override fun next(isAudioCompleted: Boolean, origin: PlaybackOrigin) {
        val current = mutableState.value
        current.nextTrackCandidate()?.let { playResolved(it, origin, isAudioCompleted) }
            ?: run { playState(PlayState.STOPPED, origin) }
    }

    actual override fun previous(origin: PlaybackOrigin) {
        mutableState.value.previousTrackCandidate()?.let { play(it, origin) }
    }

    actual override fun playState(state: PlayState, origin: PlaybackOrigin) {
        val current = mutableState.value
        if (current.playState == state) return
        val el = element()
        when (state) {
            PlayState.PLAYING -> {
                mutableState.value = current.copy(playState = state)
                val track = current.current
                if (track != null && loadedTrackId == track.id && loadedGeneration == generation && el.src.isNotEmpty()) {
                    val token = generation
                    el.play().catch {
                        if (isCurrent(token, track)) mutableState.value = mutableState.value.copy(isLoading = false, playState = PlayState.PAUSED)
                        null
                    }
                } else if (track != null) {
                    if (resolvingGeneration != generation) {
                        invalidatePendingPlayback()
                        pauseForReplacement()
                        mutableState.value = mutableState.value.copy(isLoading = true)
                        resolveTrack(track, generation)
                    }
                }
            }
            PlayState.PAUSED -> {
                el.pause()
                mutableState.value = current.copy(playState = state)
            }
            PlayState.STOPPED -> {
                invalidatePendingPlayback()
                el.pause()
                pendingPosition = 0L
                if (el.src.isNotEmpty() && el.readyState > 0) el.currentTime = 0.0
                mutableState.value = current.copy(playState = state, position = 0, isLoading = false)
            }
        }
        mutableEvent.tryEmit(PlaybackEvent.PlayStateChanged(state == PlayState.PLAYING, Duration(mutableState.value.position), origin.toEventSource()))
    }

    actual override fun playMode(mode: PlayMode, origin: PlaybackOrigin) {
        if (mutableState.value.playMode == mode) return
        mutableState.value = mutableState.value.copy(playMode = mode)
        mutableEvent.tryEmit(PlaybackEvent.PlayModeChanged(mode, origin.toEventSource()))
    }

    actual override fun volume(volume: Float, isMuted: Boolean) {
        val normalized = volume.coerceIn(0f, 1f)
        element().let { it.volume = normalized.toDouble(); it.muted = isMuted }
        mutableState.value = mutableState.value.copy(volume = normalized, isMute = isMuted)
    }

    actual override fun seek(millis: Long, seekOver: () -> Unit, origin: PlaybackOrigin) {
        val position = millis.coerceAtLeast(0)
        if (mutableState.value.position == position) {
            seekOver()
            return
        }
        val el = element()
        if (mutableState.value.isLoading || loadedGeneration != generation || el.readyState <= 0) {
            pendingPosition = position
        } else {
            val duration = mutableState.value.duration
            el.currentTime = (if (duration > 0) position.coerceAtMost(duration) else position).toDouble() / 1000
        }
        mutableState.value = mutableState.value.copy(position = position)
        mutableEvent.tryEmit(PlaybackEvent.Seek(Duration(position), seekOver, origin.toEventSource()))
        seekOver()
    }

    actual override fun release() {
        invalidatePendingPlayback()
        scope.cancel()
        audio?.pause()
        audio?.src = ""
        audio = null
        mutableState.value = PlayerState()
    }

    actual override fun nextTrack(): Track? = mutableState.value.nextTrackCandidate()
    actual override fun previousTrack(): Track? = mutableState.value.previousTrackCandidate()

    actual override fun sync(syncState: SyncState) {
        syncState.trackList?.let { queue(it) {} }
        syncState.playMode?.let { playMode(it, PlaybackOrigin.RemoteSync) }
        syncState.track?.let { play(it, PlaybackOrigin.RemoteSync) }
        syncState.position?.let { seek(it, {}, PlaybackOrigin.RemoteSync) }
        syncState.playState?.let { playState(it, PlaybackOrigin.RemoteSync) }
    }

    actual override fun addTrack(media: Track, index: Int) { mutableState.value = mutableState.value.withTrackAdded(media, index) }
    actual override fun updateQueue(tracks: List<Track>) {
        mutableState.value = mutableState.value.copy(queue = tracks, index = tracks.indexOfFirst { it.id == mutableState.value.current?.id }.coerceAtLeast(0))
    }
    actual override fun moveTrack(from: Int, to: Int) {
        val queue = mutableState.value.queue.toMutableList()
        if (from !in queue.indices || to !in queue.indices) return
        queue.add(to, queue.removeAt(from))
        updateQueue(queue)
    }
    actual override fun removeTrack(index: Int) { mutableState.value = mutableState.value.withTrackRemoved(index) }
    override fun restore(queue: List<Track>, currentId: TrackId?) {
        val current = queue.firstOrNull { it.id == currentId }
        mutableState.value = mutableState.value.copy(queue = queue, current = current, index = queue.indexOf(current).coerceAtLeast(0), playState = PlayState.PAUSED)
    }
}

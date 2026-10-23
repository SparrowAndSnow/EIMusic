package com.eimsound.eimusic.mpris

import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.util.TuiLogger
import com.monkopedia.sdbus.*
import kotlinx.coroutines.*

class MprisService(private val controller: IMediaPlayerController) : MprisManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var conn: Connection? = null
    private var obj: Object? = null
    private var prevState: PlayerState? = null

    override fun start() {
        scope.launch { init() }
    }

    override fun stop() {
        conn?.release()
        scope.cancel()
        obj = null
        conn = null
    }

    private suspend fun init() {
        try {
            val connection = createSessionBusConnection()
            conn = connection
            val dbusObj = createObject(connection, ObjectPath("/org/mpris/MediaPlayer2"))
            obj = dbusObj
            registerInterfaces(dbusObj)
            connection.requestName(ServiceName("org.mpris.MediaPlayer2.eimusic"))
            connection.startEventLoop()
            TuiLogger.log("MPRIS: sdbus-kotlin initialized")
            controller.state.collect { state ->
                val prev = prevState
                prevState = state
                if (prev != null) {
                    runCatching { detectChanges(prev, state) }
                        .onFailure { TuiLogger.log("MPRIS emit failed: ${it.message}") }
                }
            }
        } catch (e: Exception) {
            TuiLogger.log("MPRIS failed: ${e.message}")
        }
    }

    private fun registerInterfaces(o: Object) {
        o.addVTable(IFACE_ROOT) {
            prop(PropertyName("Identity")) { withGetter { "EIMusic TUI" } }
            prop(PropertyName("DesktopEntry")) { withGetter { "eimuisc" } }
            prop(PropertyName("CanQuit")) { withGetter { false } }
            prop(PropertyName("CanRaise")) { withGetter { false } }
            prop(PropertyName("HasTrackList")) { withGetter { false } }
        }
        o.addVTable(IFACE_PLAYER) {
            prop(PropertyName("PlaybackStatus")) { withGetter { playbackStatus() } }
            prop(PropertyName("LoopStatus")) {
                withGetter { loopStatus() }
                withSetter { value: String -> setLoopStatus(value) }
            }
            prop(PropertyName("Shuffle")) {
                withGetter { isShuffle() }
                withSetter { value: Boolean -> setShuffle(value) }
            }
            prop(PropertyName("Metadata")) { withGetter { buildMetadata() } }
            prop(PropertyName("Volume")) {
                withGetter { volume() }
                withSetter { value: Double -> controller.volume(value.toFloat().coerceIn(0f, 1f), false) }
            }
            prop(PropertyName("Position")) { withGetter { positionUs() } }
            prop(PropertyName("MinimumRate")) { withGetter { 1.0 } }
            prop(PropertyName("MaximumRate")) { withGetter { 1.0 } }
            prop(PropertyName("CanGoNext")) { withGetter { true } }
            prop(PropertyName("CanGoPrevious")) { withGetter { true } }
            prop(PropertyName("CanPlay")) { withGetter { true } }
            prop(PropertyName("CanPause")) { withGetter { true } }
            prop(PropertyName("CanSeek")) { withGetter { true } }
            prop(PropertyName("CanControl")) { withGetter { true } }
            method(MethodName("Next")) { call { -> controller.next() } }
            method(MethodName("Previous")) { call { -> controller.previous() } }
            method(MethodName("Play")) { call { -> if (controller.state.value.playState != PlayState.PLAYING) controller.playState(PlayState.PLAYING) } }
            method(MethodName("Pause")) { call { -> if (controller.state.value.playState != PlayState.PAUSED) controller.playState(PlayState.PAUSED) } }
            method(MethodName("PlayPause")) { call { -> controller.playState(if (controller.state.value.playState == PlayState.PLAYING) PlayState.PAUSED else PlayState.PLAYING) } }
            method(MethodName("Stop")) { call { -> controller.playState(PlayState.STOPPED) } }
            method(MethodName("Seek")) { inputParamNames = listOf("Offset"); call { offset: Long -> val pos = (controller.state.value.position + offset / 1000L).coerceAtLeast(0); controller.seek(pos) } }
            method(MethodName("SetPosition")) {
                inputParamNames = listOf("TrackId", "Position")
                call { trackId: ObjectPath, pos: Long ->
                    if (trackId.value == currentTrackPath()) controller.seek(pos / 1000L)
                }
            }
            signal(SignalName("Seeked")) { with<Long>("Position") }
        }
    }

    private fun detectChanges(prev: PlayerState, state: PlayerState) {
        val dbusObj = obj ?: return
        val changed = mutableListOf<PropertyName>()
        if (state.playState != prev.playState) changed.add(PropertyName("PlaybackStatus"))
        if (state.playMode != prev.playMode) { changed.add(PropertyName("LoopStatus")); changed.add(PropertyName("Shuffle")) }
        if (state.current?.id != prev.current?.id || state.duration != prev.duration) changed.add(PropertyName("Metadata"))
        if (kotlin.math.abs(state.volume - prev.volume) > 0.01f) changed.add(PropertyName("Volume"))
        if (changed.isNotEmpty()) dbusObj.emitPropertiesChangedSignal(IFACE_PLAYER, changed)
        if (kotlin.math.abs(state.position - prev.position) > 2000L) {
            dbusObj.emitSignal(IFACE_PLAYER, SignalName("Seeked")) {
                call { positionUs() }
            }
        }
    }

    private fun playbackStatus(): String = when (controller.state.value.playState) { PlayState.PLAYING -> "Playing"; PlayState.PAUSED -> "Paused"; PlayState.STOPPED -> "Stopped" }
    private fun loopStatus(): String = when (val m = controller.state.value.playMode) { is PlayMode.Shuffle -> "None"; is PlayMode.Loop -> "Playlist"; is PlayMode.RepeatOne -> "Track" }
    private fun isShuffle(): Boolean = controller.state.value.playMode is PlayMode.Shuffle
    private fun volume(): Double = controller.state.value.volume.toDouble()
    private fun positionUs(): Long = controller.state.value.position * 1000L

    private fun buildMetadata(): Map<String, Variant> {
        val track = controller.state.value.current ?: return emptyMap()
        val st = controller.state.value
        val m = mutableMapOf<String, Variant>()
        m["mpris:trackid"] = Variant(ObjectPath(currentTrackPath()))
        val lengthMs = track.duration?.milliseconds ?: st.duration
        m["mpris:length"] = Variant((lengthMs * 1000L).coerceAtLeast(0))
        m["xesam:title"] = Variant(track.name)
        track.artists?.let { m["xesam:artist"] = Variant(it.map { a -> a.name }) }
        track.album?.let {
            m["xesam:album"] = Variant(it.name)
            it.image?.let { img -> m["mpris:artUrl"] = Variant(if (img.contains("://")) img else "file://$img") }
        }
        track.uri?.let { m["xesam:url"] = Variant(if (it.contains("://")) it else "file://$it") }
        return m
    }

    private fun currentTrackPath(): String {
        val track = controller.state.value.current ?: return "/org/mpris/MediaPlayer2/TrackList/NoTrack"
        return "/org/eimusic/track/${track.id.source}/${sanitizeObjectPathElement(track.id.id)}"
    }

    private fun setLoopStatus(value: String) {
        val mode = when (value) {
            "Track" -> PlayMode.RepeatOne
            "Playlist" -> PlayMode.Loop
            else -> return
        }
        controller.playMode(mode)
    }

    private fun setShuffle(value: Boolean) {
        if (value) {
            if (controller.state.value.playMode !is PlayMode.Shuffle) {
                val count = controller.state.value.queue.size
                val seed = kotlin.random.Random.nextLong()
                controller.playMode(PlayMode.Shuffle(seed, com.eimsound.eimusic.music.generateShuffleIndices(count, seed)))
            }
        } else if (controller.state.value.playMode is PlayMode.Shuffle) {
            controller.playMode(PlayMode.Loop)
        }
    }

    private fun sanitizeObjectPathElement(value: String): String =
        value.map { if (it.isLetterOrDigit() || it == '_') it else '_' }.joinToString("")

    companion object {
        private val IFACE_ROOT = InterfaceName("org.mpris.MediaPlayer2")
        private val IFACE_PLAYER = InterfaceName("org.mpris.MediaPlayer2.Player")
    }
}

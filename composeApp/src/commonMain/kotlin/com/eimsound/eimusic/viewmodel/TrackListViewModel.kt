// TrackListViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.events.TrackListEvent
import com.eimsound.eimusic.music.Track
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI状态数据类
 */
data class TrackListState(
    val currentTrack: Track? = null,
)

/**
 * 通用的 TrackListViewModel，支持不同类型的音轨列表展示
 */
class TrackListViewModel() : ViewModel() {
    init{
        viewModelScope.launch {
            EventBus.PlayingListEventBus.receive {
                when(it){
                    is PlayingListEvent.TrackChanged-> {
                        _state.value = state.value.copy(currentTrack = it.track)
                    }
                }
            }
        }
    }

    private val _state = MutableStateFlow(TrackListState())
    val state: StateFlow<TrackListState> = _state.asStateFlow()

    /**
     * 选择音轨
     */
    fun selectTrack(track: Track) {
        EventBus.TrackListEventBus.send(TrackListEvent.TrackSelected(track))
    }

    /**
     * 播放音轨
     */
    fun play(tracks: List<Track>, track: Track) {
        EventBus.TrackListEventBus.send(TrackListEvent.PlayTrackList(tracks, track))
    }

    /**
     * 添加到播放队列
     */
    fun addToQueue(track: Track) {
        EventBus.TrackListEventBus.send(TrackListEvent.AddedToQueue(track))
    }

}
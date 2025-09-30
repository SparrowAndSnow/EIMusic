// TrackListViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 通用的 TrackListViewModel，支持不同类型的音轨列表展示
 */
class TrackListViewModel<T : Any>(
    private val repository: TrackRepository<T>
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackListUiState<T>())
    val uiState: StateFlow<TrackListUiState<T>> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<TrackListEvent>()
    val event: SharedFlow<TrackListEvent> = _event.asSharedFlow()

    private var currentQuery: String? = null
    private var currentParams: Map<String, Any>? = null

    /**
     * 加载音轨列表
     */
    fun loadTracks(params: Map<String, Any>? = null) {
        viewModelScope.launch {
            updateUiState { it.copy(isLoading = true, error = null) }
            
            try {
                val tracks = repository.loadTracks(params ?: emptyMap())
                updateUiState { 
                    it.copy(
                        isLoading = false,
                        tracks = tracks,
                        isEmpty = tracks.isEmpty()
                    ) 
                }
                currentParams = params
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * 搜索音轨
     */
    fun searchTracks(query: String) {
        if (query.isBlank()) {
            loadTracks(currentParams)
            return
        }

        viewModelScope.launch {
            updateUiState { it.copy(isLoading = true, error = null) }
            
            try {
                val tracks = repository.searchTracks(query)
                updateUiState { 
                    it.copy(
                        isLoading = false,
                        tracks = tracks,
                        isEmpty = tracks.isEmpty()
                    ) 
                }
                currentQuery = query
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * 刷新列表
     */
    fun refresh() {
        when {
            !currentQuery.isNullOrBlank() -> searchTracks(currentQuery!!)
            else -> loadTracks(currentParams)
        }
    }

    /**
     * 选择音轨
     */
    fun selectTrack(track: T) {
        viewModelScope.launch {
            _event.emit(TrackListEvent.TrackSelected(track))
        }
    }

    /**
     * 播放音轨
     */
    fun playTrack(track: T) {
        viewModelScope.launch {
            try {
                repository.playTrack(track)
                _event.emit(TrackListEvent.TrackPlayed(track))
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * 添加到播放队列
     */
    fun addToQueue(track: T) {
        viewModelScope.launch {
            try {
                repository.addToQueue(track)
                _event.emit(TrackListEvent.AddedToQueue(track))
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleError(exception: Exception) {
        viewModelScope.launch {
            updateUiState { it.copy(isLoading = false, error = exception.message) }
            _event.emit(TrackListEvent.Error(exception))
        }
    }

    private fun updateUiState(updater: (TrackListUiState<T>) -> TrackListUiState<T>) {
        _uiState.value = updater(_uiState.value)
    }
}

/**
 * UI状态数据类
 */
data class TrackListUiState<T>(
    val tracks: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false
)

/**
 * 事件密封类
 */
sealed class TrackListEvent {
    data class TrackSelected<T>(val track: T) : TrackListEvent()
    data class TrackPlayed<T>(val track: T) : TrackListEvent()
    data class AddedToQueue<T>(val track: T) : TrackListEvent()
    data class Error(val exception: Exception) : TrackListEvent()
}

/**
 * 音轨仓库接口，定义数据操作
 */
interface TrackRepository<T> {
    suspend fun loadTracks(params: Map<String, Any>): List<T>
    suspend fun searchTracks(query: String): List<T>
    suspend fun playTrack(track: T)
    suspend fun addToQueue(track: T)
}

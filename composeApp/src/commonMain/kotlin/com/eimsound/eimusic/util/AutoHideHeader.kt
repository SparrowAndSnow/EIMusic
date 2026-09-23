package com.eimsound.eimusic.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

class AutoHideHeaderState(initialVisible: Boolean = true) {
    var isVisible by mutableStateOf(initialVisible)
}

@Composable
fun rememberAutoHideHeaderState(initialVisible: Boolean = true): AutoHideHeaderState {
    return rememberSaveable(
        saver = Saver(
        save = { it.isVisible },
        restore = { AutoHideHeaderState(it) }
    )) {
        AutoHideHeaderState(initialVisible)
    }
}

@Composable
fun rememberAutoHideHeaderConnection(
    state: LazyListState,
    headerState: AutoHideHeaderState = rememberAutoHideHeaderState()
): NestedScrollConnection {
    val nestedScrollConnection = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                val isAtTop = state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
                val cannotScroll = !state.canScrollForward && !state.canScrollBackward

                // 手指往上拨 (delta < 0)，即列表内容往下走，准备看下面的内容 -> 隐藏头部
                if (delta > 10f && !headerState.isVisible && (isAtTop || cannotScroll)) {
                    headerState.isVisible = true
                }

                // 隐藏逻辑保持不变
                if (delta < -10f && headerState.isVisible) {
                    headerState.isVisible = false
                }
                // 手指往下拉 (delta > 0)，且列表已经已经在顶部了 -> 显示头部
                return Offset.Zero
            }

        }
    }

    LaunchedEffect(state) {
        var previousIndex = state.firstVisibleItemIndex
        var previousOffset = state.firstVisibleItemScrollOffset

        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                // 判断是否在绝对顶部
                val isAtTop = currentIndex == 0 && currentOffset == 0
                // 计算是否向下滚动 (内容向上移)
                val isScrollingDown = if (currentIndex != previousIndex) {
                    currentIndex > previousIndex
                } else {
                    currentOffset > previousOffset
                }

                if (isAtTop) {
                    headerState.isVisible = true
                } else if (isScrollingDown) {
                    // 只要不在顶部且往下滚，就隐藏
                    headerState.isVisible = false
                }

                previousIndex = currentIndex
                previousOffset = currentOffset
            }
    }
    return nestedScrollConnection
}
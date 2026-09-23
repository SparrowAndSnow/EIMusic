package com.eimsound.eimusic.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.eimsound.eimusic.util.AutoHideHeaderState
import com.eimsound.eimusic.util.rememberAutoHideHeaderConnection
import kotlin.reflect.KProperty1

@Composable
actual fun <T> ColumnList(
    modifier: Modifier,
    state: LazyListState,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    refreshingEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    flingBehavior: FlingBehavior,
    userScrollEnabled: Boolean,
    list: List<T>,
    key: KProperty1<T, *>?,
    header: @Composable () -> Unit,
    autoHideHeaderState: AutoHideHeaderState,
    item: @Composable (T) -> Unit
) {
    val nestedScrollConnection = rememberAutoHideHeaderConnection(state, autoHideHeaderState)
    AnimatedVisibility(
        visible = autoHideHeaderState.isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        header()
    }
    if (refreshingEnabled) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            LazyColumn(
                modifier.nestedScroll(nestedScrollConnection),
                state,
                contentPadding,
                reverseLayout,
                verticalArrangement,
                horizontalAlignment,
                flingBehavior,
                userScrollEnabled
            ) {
                itemsIndexed(list, key = { index, item ->
                    key?.get(item)?.toString() ?: index
                }) { _, it ->
                    item(it)
                }
            }
        }
    } else {
        LazyColumn(
            modifier.nestedScroll(nestedScrollConnection),
            state,
            contentPadding,
            reverseLayout,
            verticalArrangement,
            horizontalAlignment,
            flingBehavior,
            userScrollEnabled
        ) {
            itemsIndexed(list, key = { index, item ->
                key?.get(item)?.toString() ?: index
            }) { _, it ->
                item(it)
            }
        }
    }
}

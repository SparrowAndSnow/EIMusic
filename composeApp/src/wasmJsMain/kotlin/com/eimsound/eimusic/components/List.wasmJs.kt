package com.eimsound.eimusic.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.eimsound.eimusic.util.AutoHideHeaderState
import com.eimsound.eimusic.util.rememberAutoHideHeaderConnection
import com.eimsound.eimusic.util.rememberAutoHideHeaderState
import com.eimsound.eimusic.util.thenIf
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
    Box {
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
                key?.get(item) ?: index
            }) { _, it ->
                item(it)
            }
        }
        if (userScrollEnabled) {
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().thenIf(reverseLayout, {
                    graphicsLayer(scaleY = if (reverseLayout) -1f else 1f)
                }),
                adapter = rememberScrollbarAdapter(
                    scrollState = state
                ),
                style = LocalScrollbarStyle.current.copy(
                    hoverColor = MaterialTheme.colorScheme.primary,
                    unhoverColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
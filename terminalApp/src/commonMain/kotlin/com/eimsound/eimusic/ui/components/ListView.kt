package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.eimsound.eimusic.keyboard.ListViewScreenAction
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.keyboard.dispatchListViewKey
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.layout.MomentumScrollState
import com.jakewharton.mosaic.layout.MouseEvent
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.clickable
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.hoverable
import com.jakewharton.mosaic.layout.onGloballyPositioned
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.onMouseEvent
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Spacer
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextOverflow
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/** 列表选中项和滚轮位置；可由页面持有以在外部移动/删除后跳转。 */
@Stable
class ListSelectionState(initialIndex: Int = 0) {
    var index by mutableIntStateOf(initialIndex)
        private set

    internal val scroll = MomentumScrollState()

    /** 只更新选中项，不重置滚轮位置（动画推进用）。 */
    internal fun select(index: Int) {
        this.index = index
    }

    /** 键盘/点击/外部移动：选中并立即定位。 */
    fun jumpTo(index: Int) {
        this.index = index
        scroll.jumpTo(index)
    }
}

@Composable
fun rememberListSelectionState(key: Any? = Unit): ListSelectionState =
    remember(key) { ListSelectionState() }

/** 滚轮一次移动的选择行数。 */
private const val WheelStep = 3


@Composable
fun <T> ListView(
    items: List<T>,
    visibleLines: Int,
    render: @Composable (item: T, index: Int, isSelected: Boolean) -> Unit,
    emptyMessage: String = " -- empty -- ",
    showScrollIndicator: Boolean = true,
    enabled: Boolean = true,
    onConfirm: (T) -> Unit = {},
    onSelectedChange: (Int) -> Unit = {},
    onItemSecondaryClick: ((T, Int) -> Unit)? = null,
    /** 选中项与滚轮位置；页面可持有一份以在外部移动/删除后跳转。 */
    selection: ListSelectionState = rememberListSelectionState(items),
) {
    val scroll = selection.scroll
    val selectedIndex = selection.index.coerceIn(0, items.lastIndex.coerceAtLeast(0))

    fun select(index: Int) {
        val clamped = index.coerceIn(0, items.lastIndex.coerceAtLeast(0))
        selection.jumpTo(clamped)
        onSelectedChange(clamped)
    }

    LaunchedEffect(items.size) {
        val target = if (items.isEmpty()) 0 else selection.index.coerceIn(0, items.lastIndex)
        if (target != selection.index) {
            selection.jumpTo(target)
            onSelectedChange(target)
        }
    }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    var listTop by remember { mutableIntStateOf(0) }
    val uiInput = LocalUiInputEnabled.current
    val palette = LocalTuiColors.current

    // 动画期间窗口跟着小数位置移动，行内容每跨过一行才真正重绘，看起来是逐行平滑滚动。
    val visualAnchor = if (items.isEmpty()) 0 else scroll.position.roundToInt().coerceIn(0, items.lastIndex)
    val scrollOffset = remember(visualAnchor, visibleLines, items.size) {
        val max = (items.size - visibleLines).coerceAtLeast(0)
        (visualAnchor - visibleLines / 2).coerceIn(0, max)
    }

    LaunchedEffect(items) {
        while (true) {
            // 空闲时不占帧；库里的状态有惯性时才按帧推进。
            snapshotFlow { scroll.isMoving }.first { it }
            var lastNanos = 0L
            while (scroll.isMoving) {
                withFrameNanos { now ->
                    if (items.isEmpty()) {
                        scroll.jumpTo(0)
                        return@withFrameNanos
                    }
                    if (lastNanos != 0L) {
                        val dt = (now - lastNanos) / 1_000_000_000.0.toFloat()
                        scroll.advance(dt, items.lastIndex.toFloat())
                        val index = scroll.position.roundToInt().coerceIn(0, items.lastIndex)
                        if (index != selection.index) {
                            selection.select(index)
                            onSelectedChange(index)
                        }
                        hoveredIndex = -1
                    }
                    lastNanos = now
                }
            }
        }
    }

    Box(
        Modifier
            .onGloballyPositioned { position, _ -> listTop = position.y }
            .onKeyEvent { event ->
                if (!enabled || !uiInput) return@onKeyEvent false
                val action =
                    dispatchListViewKey(
                        event,
                        selection.index,
                        { index -> select(index) },
                        items.lastIndex,
                    )
                when (action) {
                    ListViewScreenAction.Confirm -> {
                        items.getOrNull(selection.index)?.let(onConfirm); true
                    }
                    null -> false
                }
            }
            .onMouseEvent { event ->
                if (!enabled || !uiInput || items.isEmpty()) return@onMouseEvent false
                if (event.type != MouseEvent.Type.Press) return@onMouseEvent false
                if (event.button == MouseEvent.Button.Right && onItemSecondaryClick != null) {
                    val row = event.y - listTop + scrollOffset
                    val item = items.getOrNull(row) ?: return@onMouseEvent false
                    select(row)
                    onItemSecondaryClick(item, row)
                    return@onMouseEvent true
                }
                when (event.button) {
                    MouseEvent.Button.WheelUp, MouseEvent.Button.WheelDown -> {
                        // 一次滚轮给一个约 WheelStep 行的初速度，之后由摩擦衰减形成渐停。
                        val rows = if (event.button == MouseEvent.Button.WheelUp) -WheelStep else WheelStep
                        scroll.impulse(rows)
                        hoveredIndex = -1
                        true
                    }
                    else -> false
                }
            }
    ) {
        if (items.isEmpty()) {
            Text(emptyMessage, color = palette.textDimFg)
            return@Box
        }

        Column {
            val end = (scrollOffset + visibleLines).coerceAtMost(items.size)
            val shown = end - scrollOffset

            for (i in scrollOffset until end) {
                val item = items[i]
                val isSelected = i == selectedIndex
                val hovered = hoveredIndex == i
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(if (hovered && !isSelected) palette.hoverBg else Color.Unspecified)
                        .hoverable(enabled = uiInput) { inside ->
                            if (inside) {
                                hoveredIndex = i
                            } else if (hoveredIndex == i) {
                                hoveredIndex = -1
                            }
                        }
                        // 单击只负责选中；同一行 400ms 内再次点击才触发确认。
                        .clickable(
                            enabled = uiInput && enabled,
                            onDoubleClick = { onConfirm(item) },
                            onClick = { select(i) },
                        )
                ) {
                    render(item, i, isSelected)
                }
            }

            if (showScrollIndicator && items.size > visibleLines) {
                val total = items.size
                val posLabel = "${scrollOffset + 1}-${end}/$total"
                repeat((visibleLines - shown).coerceAtLeast(0)) { Text("", color = palette.textDimFg) }
                Text("${2.spacer}$posLabel", color = palette.textDimFg)
            }
        }
    }
}

/**
 * Default list item renderer: yazi-style with `▸` for selected,
 * ` ` for unselected, index number, name, and artist.
 *
 * name/artist 使用固定宽度槽位，超长内容由 Text 以省略号截断。
 */
@Composable
fun TrackListItem(
    index: Int,
    isSelected: Boolean,
    isCurrent: Boolean,
    number: Int,
    name: String,
    artist: String,
    nameMax: Int = 50,
    artistMax: Int = 24,
) {
    val palette = LocalTuiColors.current
    val marker = if (isCurrent) "▶" else if (isSelected) "▸" else " "
    val markerColor = when {
        isCurrent -> palette.highlightFg
        isSelected -> palette.currentMarkerFg
        else -> palette.textDimFg
    }
    val textColor = if (isSelected || isCurrent) palette.highlightFg else palette.textFg

    Row(Modifier.fillMaxWidth()) {
        Text(marker, color = markerColor)
        Spacer(Modifier.width(1))
        Text("#$number", color = palette.textDimFg)
        Spacer(Modifier.width(1))
        Box(Modifier.width(nameMax)) {
            Text(name, Modifier.fillMaxWidth(), color = textColor, softWrap = true, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (artist.isNotEmpty()) {
            Text("  —  ", color = palette.textDimFg)
            Box(Modifier.width(artistMax)) {
                Text(artist, Modifier.fillMaxWidth(), color = palette.textDimFg, softWrap = true, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

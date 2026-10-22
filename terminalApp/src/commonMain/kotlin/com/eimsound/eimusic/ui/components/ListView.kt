package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.eimsound.eimusic.keyboard.ListViewScreenAction
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.keyboard.dispatchListViewKey
import com.eimsound.eimusic.ui.input.ClickTracker
import com.eimsound.eimusic.ui.input.MomentumScroll
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.layout.MouseEvent
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.hoverable
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
import kotlin.time.Clock
import kotlinx.coroutines.flow.first

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
) {
    var selectedIndex by remember(items) { mutableIntStateOf(0) }
    val scroll = remember(items) { MomentumScroll() }
    var visualPos by remember(items) { mutableFloatStateOf(0f) }
    var animating by remember(items) { mutableStateOf(false) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    val clickTracker = remember { ClickTracker() }
    val uiInput = LocalUiInputEnabled.current
    val palette = LocalTuiColors.current

    // 动画期间窗口跟着小数位置移动，行内容每跨过一行才真正重绘，看起来是逐行平滑滚动。
    val visualAnchor = if (items.isEmpty()) 0 else visualPos.roundToInt().coerceIn(0, items.lastIndex)
    val scrollOffset = remember(visualAnchor, visibleLines, items.size) {
        val max = (items.size - visibleLines).coerceAtLeast(0)
        (visualAnchor - visibleLines / 2).coerceIn(0, max)
    }

    LaunchedEffect(items) {
        while (true) {
            // 空闲时不占帧；有惯性时才按帧推进。
            snapshotFlow { animating }.first { it }
            var lastNanos = 0L
            while (animating) {
                withFrameNanos { now ->
                    if (items.isEmpty()) {
                        scroll.jumpTo(0)
                        visualPos = 0f
                        animating = false
                        return@withFrameNanos
                    }
                    if (lastNanos != 0L) {
                        val dt = (now - lastNanos) / 1_000_000_000.0.toFloat()
                        val moving = scroll.advance(dt, items.lastIndex.toFloat())
                        visualPos = scroll.position
                        val index = scroll.position.roundToInt().coerceIn(0, items.lastIndex)
                        if (index != selectedIndex) {
                            selectedIndex = index
                            onSelectedChange(index)
                        }
                        hoveredIndex = -1
                        if (!moving) animating = false
                    }
                    lastNanos = now
                }
            }
        }
    }

    Box(
        Modifier
            .onKeyEvent { event ->
                if (!enabled || !uiInput) return@onKeyEvent false
                val action =
                    dispatchListViewKey(
                        event,
                        selectedIndex,
                        { index ->
                            selectedIndex = index
                            scroll.jumpTo(index)
                            visualPos = index.toFloat()
                            animating = false
                            onSelectedChange(index)
                        },
                        items.lastIndex,
                    )
                when (action) {
                    ListViewScreenAction.Confirm -> {
                        items.getOrNull(selectedIndex)?.let(onConfirm); true
                    }
                    null -> false
                }
            }
            .onMouseEvent { event ->
                if (!enabled || !uiInput || items.isEmpty()) return@onMouseEvent false
                if (event.type != MouseEvent.Type.Press) return@onMouseEvent false
                when (event.button) {
                    MouseEvent.Button.WheelUp, MouseEvent.Button.WheelDown -> {
                        // 一次滚轮给一个约 WheelStep 行的初速度，之后由摩擦衰减形成渐停。
                        val rows = if (event.button == MouseEvent.Button.WheelUp) -WheelStep else WheelStep
                        scroll.impulse(rows)
                        animating = true
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
                        .onMouseEvent { event ->
                            if (uiInput && enabled && event.type == MouseEvent.Type.Press && event.button == MouseEvent.Button.Left) {
                                // 单击只负责选中；同一行短时间内再次按下才算双击并触发确认。
                                val now = Clock.System.now().toEpochMilliseconds()
                                val doubleClick = clickTracker.press(i, now)
                                selectedIndex = i
                                scroll.jumpTo(i)
                                visualPos = i.toFloat()
                                animating = false
                                onSelectedChange(i)
                                if (doubleClick) onConfirm(item)
                                true
                            } else {
                                false
                            }
                        }
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

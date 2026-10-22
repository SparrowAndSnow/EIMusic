package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.eimsound.eimusic.keyboard.ListViewScreenAction
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.keyboard.dispatchListViewKey
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
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.coroutines.flow.first

/** 滚轮一次移动的选择行数。 */
private const val WheelStep = 3

/** 双击判定窗口（毫秒）。 */
private const val DoubleClickMillis = 400L

/** 滚动摩擦（1/s）：速度按 e^(-friction*dt) 衰减，越大停得越快。 */
private const val ScrollFriction = 3f

/** 滚轮速度上限（行/秒）。 */
private const val MaxScrollSpeed = 60f

/** 低于这个速度直接停住，避免无限小数抖动。 */
private const val MinScrollSpeed = 0.5f

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
    var visualPos by remember(items) { mutableFloatStateOf(0f) }
    var scrollVelocity by remember(items) { mutableFloatStateOf(0f) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    val uiInput = LocalUiInputEnabled.current
    var lastClickRow by remember { mutableIntStateOf(-1) }
    var lastClickAt by remember { mutableLongStateOf(0L) }
    val palette = LocalTuiColors.current

    // 动画期间窗口跟着小数位置移动，行内容每跨过一行才真正重绘，看起来是逐行平滑滚动。
    val visualAnchor = if (items.isEmpty()) 0 else visualPos.roundToInt().coerceIn(0, items.lastIndex)
    val scrollOffset = remember(visualAnchor, visibleLines, items.size) {
        val max = (items.size - visibleLines).coerceAtLeast(0)
        (visualAnchor - visibleLines / 2).coerceIn(0, max)
    }

    LaunchedEffect(items) {
        while (true) {
            // 空闲时不占帧；有速度时才按帧推进。
            snapshotFlow { scrollVelocity }.first { it != 0f }
            var lastNanos = 0L
            while (scrollVelocity != 0f) {
                withFrameNanos { now ->
                    if (items.isEmpty()) {
                        scrollVelocity = 0f
                        return@withFrameNanos
                    }
                    if (lastNanos != 0L) {
                        val dt = ((now - lastNanos) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
                        val max = items.lastIndex.toFloat()
                        var next = visualPos + scrollVelocity * dt
                        if (next <= 0f) {
                            next = 0f
                            scrollVelocity = 0f
                        }
                        if (next >= max) {
                            next = max
                            scrollVelocity = 0f
                        }
                        visualPos = next
                        val index = next.roundToInt().coerceIn(0, items.lastIndex)
                        if (index != selectedIndex) {
                            selectedIndex = index
                            onSelectedChange(index)
                        }
                        scrollVelocity *= exp(-ScrollFriction * dt)
                        if (abs(scrollVelocity) < MinScrollSpeed) scrollVelocity = 0f
                        hoveredIndex = -1
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
                            visualPos = index.toFloat()
                            scrollVelocity = 0f
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
                        scrollVelocity = (scrollVelocity + rows * ScrollFriction)
                            .coerceIn(-MaxScrollSpeed, MaxScrollSpeed)
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
                                val doubleClick = i == lastClickRow && now - lastClickAt in 0L..DoubleClickMillis
                                selectedIndex = i
                                visualPos = i.toFloat()
                                scrollVelocity = 0f
                                onSelectedChange(i)
                                if (doubleClick) onConfirm(item)
                                lastClickRow = i
                                lastClickAt = now
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

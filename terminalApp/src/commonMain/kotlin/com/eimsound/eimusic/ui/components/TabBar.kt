package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.MouseEvent
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.hoverable
import com.jakewharton.mosaic.layout.onMouseEvent
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextOverflow

/** 底部标签栏的一项：标签、是否当前页、点击回调。 */
data class TabItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * 底部页面标签栏，风格与 rrtop 的 BottomStatusBar 一致：
 * 左侧为可点击的页面标签，右侧显示当前页面的按键提示。
 */
@Composable
fun TabBar(
    tabs: List<TabItem>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalTuiColors.current
    val hint by LocalStatusBarHint.current
    var hovered by remember { mutableIntStateOf(-1) }

    Row(modifier.fillMaxWidth().background(palette.statusBarBg)) {
        tabs.forEachIndexed { index, tab ->
            val background = when {
                tab.selected -> palette.selectionBg
                hovered == index -> palette.hoverBg
                else -> Color.Unspecified
            }
            Box(
                Modifier
                    .background(background)
                    .hoverable { inside ->
                        hovered = if (inside) index else if (hovered == index) -1 else hovered
                    }
                    .onMouseEvent { event ->
                        if (event.type == MouseEvent.Type.Press && event.button == MouseEvent.Button.Left) {
                            tab.onClick()
                            true
                        } else {
                            false
                        }
                    }
                    .padding(horizontal = 1)
            ) {
                Text(
                    tab.label,
                    color = if (tab.selected) palette.highlightFg else palette.statusBarFg,
                )
            }
            if (index < tabs.lastIndex) {
                Text("│", color = palette.dimBorderFg)
            }
        }
        if (hint.isNotBlank()) {
            Text(
                "  $hint",
                modifier = Modifier.weight(1f),
                color = palette.statusBarFg,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

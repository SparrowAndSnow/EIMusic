package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.FocusRequester
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.focusRequester
import com.jakewharton.mosaic.layout.MouseEvent
import com.jakewharton.mosaic.layout.horizontalScroll
import com.jakewharton.mosaic.layout.onMouseEvent
import com.jakewharton.mosaic.layout.onPreviewKeyEvent
import com.jakewharton.mosaic.layout.rememberScrollState
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.BasicTextField
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextOverflow

/**
 * 单行文本输入框，基于 Mosaic 的 [BasicTextField]。
 *
 * - 由 BasicTextField 自己负责字素簇光标、IME、粘贴与焦点；不再手工拆行/画光标块。
 * - 视口宽度随内容增长（最短 30 格，最长到内容区宽度），超出部分横向滚动并自动贴到末尾。
 * - Enter 触发 [onConfirm]，Escape 触发 [onCancel]；Up/Down/Tab/翻页键在输入期间被吞掉，
 *   避免误触底层列表导航。
 * - [enabled] 与 [showCursor] 同时为真时才可编辑并显示光标。
 */
@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    showCursor: Boolean = true,
    enabled: Boolean = true,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val palette = LocalTuiColors.current
    val safeArea = LocalSafeContentSize.current
    val active = enabled && showCursor && LocalUiInputEnabled.current
    val requester = remember { FocusRequester() }
    val scroll = rememberScrollState()

    // 视口随输入增长；CJK/emoji 用字符数估算可能偏窄，横滚负责兜底。
    val maxFieldWidth = (safeArea.columns - 4).coerceAtLeast(20)
    val viewportWidth = (value.length + 2).coerceIn(30, maxFieldWidth)

    LaunchedEffect(active) {
        if (active) requester.requestFocus()
    }
    LaunchedEffect(scroll.maxValue, value) {
        scroll.scrollTo(scroll.maxValue)
    }

    Box(
        Modifier
            .width(viewportWidth)
            .onMouseEvent { event ->
                // 点击输入框（含占位符区域）把焦点拉回来；按下即响应，不依赖 press/release 跨帧配对。
                if (active && event.type == MouseEvent.Type.Press && event.button == MouseEvent.Button.Left) {
                    requester.requestFocus()
                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { event ->
                if (!active) return@onPreviewKeyEvent false
                when (event.key) {
                    "Escape" -> {
                        onCancel()
                        true
                    }
                    "Enter" -> {
                        onConfirm()
                        true
                    }
                    // 单行输入期间这些键交给输入框自己吞掉，不冒泡到屏幕级快捷键。
                    "ArrowUp", "ArrowDown", "PageUp", "PageDown", "Tab" -> true
                    else -> false
                }
            }
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                buildAnnotatedString {
                    // 前导空格让 BasicTextField 的反显光标落在这一格，而不是吃掉占位符首字符。
                    withStyle(SpanStyle(color = palette.textDimFg)) { append(" $placeholder") }
                },
                Modifier.fillMaxWidth(),
                color = palette.textDimFg,
                softWrap = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.focusRequester(requester),
                enabled = active,
                color = palette.textFg,
            )
        }
    }
}

package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.ui.Icon
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextOverflow

@Composable
fun HeaderBar(
    title: String,
    state: PlayerState,
    authViewModel: AuthViewModel,
    isDark: Boolean,
    onNavigate: () -> Unit,
    channelLabel: String? = null,
) {
    val palette = LocalTuiColors.current
    val authState by authViewModel.authState.bridgeToMutable()
    val authLabel = when (val s = authState) {
        is AuthState.Authenticated -> s.user.username
        is AuthState.Loading -> "..."
        else -> "login"
    }
    val queue = "${state.queue.size}"
    val themeIcon = if (isDark) Icon.Moon.sym else Icon.Sun.sym

    Box(modifier = Modifier.fillMaxWidth().background(palette.headerBg)) {
        Text(
            buildAnnotatedString {
                spacer(1)
                withStyle(SpanStyle(color = palette.titleFg)) { append(title) }
                spacer(2)
                withStyle(SpanStyle(color = palette.modeFg)) { append(themeIcon) }
                spacer(2)
                playControlSegments(state, palette)
                spacer(2)
                withStyle(SpanStyle(color = palette.textDimFg)) { append(Icon.Queue.sym) }
                spacer(1)
                withStyle(SpanStyle(color = palette.textFg)) { append(queue) }
                spacer(2)
                withStyle(SpanStyle(color = palette.textDimFg)) { append(Icon.User.sym) }
                spacer(1)
                withStyle(SpanStyle(color = palette.highlightFg)) { append(authLabel) }
                (spacer(2))
                if (channelLabel != null) {
                    withStyle(SpanStyle(color = palette.highlightFg)) { append(Icon.Together.sym) }
                    spacer(1)
                    withStyle(SpanStyle(color = palette.highlightFg)) { append(channelLabel) }
                }
            },
            // 终端宽度交给 Text 的 maxLines + Ellipsis 处理：不再手算列宽/截断。
            Modifier.fillMaxWidth(),
            color = palette.textFg,
            softWrap = true,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

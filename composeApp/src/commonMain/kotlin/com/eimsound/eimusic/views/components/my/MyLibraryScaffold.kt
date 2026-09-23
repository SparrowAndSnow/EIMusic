package com.eimsound.eimusic.views.components.my

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii

private val CompactBreakPoint = 980.dp

@Composable
fun MyLibraryScaffold(
    modifier: Modifier = Modifier,
    isCompactDetailVisible: Boolean,
    collectionPane: @Composable () -> Unit,
    detailPane: @Composable (showCompactBack: Boolean) -> Unit,
) {
    val palette = LocalPalette.current
    val radii = LocalRadii.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < CompactBreakPoint

        if (isCompact) {
            AnimatedContent(
                targetState = isCompactDetailVisible,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInHorizontally(initialOffsetX = { it / 8 })) togetherWith
                        (fadeOut(animationSpec = tween(180)) + slideOutHorizontally(targetOffsetX = { -it / 10 }))
                },
                label = "my-pane-switch"
            ) { showDetail ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (showDetail) {
                        detailPane(true)
                    } else {
                        collectionPane()
                    }
                }
            }
            return@BoxWithConstraints
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight(),
            ) {
                collectionPane()
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = radii.medium,
                color = palette.surfaceGlass,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                detailPane(false)
            }
        }
    }
}

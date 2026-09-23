package com.eimsound.eimusic.util

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp


@Composable
fun Modifier.thenIf(condition: Boolean, modifier: @Composable Modifier.() -> Modifier) =
    if (condition) then(modifier()) else this


fun Modifier.pulsatingEffect(
    currentValue: Float,
    bufferValue: Float,
    isVisible: Boolean,
    color: Color = Color.Gray,
): Modifier = composed {
    val density = LocalDensity.current
    var trackWidth by remember { mutableFloatStateOf(0f) }
    val offset = 18.dp
    val offsetDp = with(density) { offset.toPx() }
    val startOffset by remember(currentValue) {
        derivedStateOf {
            trackWidth * currentValue + offsetDp
        }
    }


    val transition = rememberInfiniteTransition(label = "trackAnimation")

    val animationProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                delayMillis = 200,
            )
        ), label = "width"
    )

    val endOffset by remember(bufferValue, currentValue) {
        derivedStateOf {
            startOffset + ((trackWidth * bufferValue - startOffset) * animationProgress)
        }
    }
    this then Modifier
        .onGloballyPositioned { coordinates ->
            trackWidth = coordinates.size.width.toFloat()
        }
        .drawWithContent {
            drawContent()

            val strokeWidth = size.height - with(density) { 2.dp.toPx() }
            val y = size.height / 2f + 1f

            val dynamicAlpha = (1f - animationProgress).coerceIn(0f, 0.5f)

            if (isVisible) {
                drawLine(
                    color = color.copy(alpha = dynamicAlpha),
                    start = Offset(startOffset, y),
                    end = Offset(endOffset, y),
                    cap = StrokeCap.Round,
                    strokeWidth = strokeWidth
                )
            }
        }
}
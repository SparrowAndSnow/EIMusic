package com.eimsound.eimusic.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class EIMusicMotion(
    val fast: FiniteAnimationSpec<Float> = tween(durationMillis = 140, easing = emphasizedEasing),
    val medium: FiniteAnimationSpec<Float> = tween(durationMillis = 240, easing = emphasizedEasing),
    val springy: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessLow),
)

private val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

val LocalMotion = staticCompositionLocalOf {
    EIMusicMotion(
        fast = tween(durationMillis = 140, easing = emphasizedEasing),
        medium = tween(durationMillis = 240, easing = emphasizedEasing),
        springy = spring(stiffness = Spring.StiffnessLow),
    )
}

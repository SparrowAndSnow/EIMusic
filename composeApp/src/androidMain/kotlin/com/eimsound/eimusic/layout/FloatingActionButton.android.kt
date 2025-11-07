package com.eimsound.eimusic.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.rememberAsyncImagePainter
import com.eimsound.eimusic.viewmodel.PlayerState
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 可拖拽的悬浮操作按钮组件
 * 支持向四个方向拖拽并吸附到功能图标执行相应操作
 */
@Composable
actual fun FloatingActionButton() {
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val playerState by playerViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 动画值，用于吸附效果
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // 状态管理
    var isDragging by remember { mutableStateOf(false) }
    var dragTarget by remember { mutableStateOf<DragTarget?>(null) }
    var showIcons by remember { mutableStateOf(false) }
    var isAdsorbed by remember { mutableStateOf(false) }

    // 获取主题颜色
    val primaryColor = MaterialTheme.colorScheme.primary

    // 组件尺寸定义 (dp单位)
    val fabSize = 72
    val iconSize = 32
    val iconDistance = 56 // 图标中心到FAB中心的距离

    // 转换为像素单位用于计算
    val fabSizePx = with(density) { fabSize.dp.toPx() }
    val iconSizePx = with(density) { iconSize.dp.toPx() }
    val iconDistancePx = with(density) { iconDistance.dp.toPx() }

    // 容器和拖拽参数
    val containerSizePx = iconDistancePx * 2 + fabSizePx
    val maxDragDistance = iconDistancePx
    val adsorptionThreshold = with(density) { 30.dp.toPx() }
    val desorptionDistance = with(density) { 30.dp.toPx() }

    // 旋转动画（播放时）
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (playerState.isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        )
    )

    // 主容器
    Box(
        modifier = Modifier
            .size(with(density) { containerSizePx.toDp() })
            .zIndex(10f)
    ) {
        // 容器中心点坐标 (像素单位)
        val containerCenterXPx = containerSizePx / 2
        val containerCenterYPx = containerSizePx / 2

        // 功能图标（拖拽时显示）
        if (showIcons) {
            FunctionIcons(
                playerState = playerState,
                dragTarget = dragTarget,
                containerCenterXPx = containerCenterXPx,
                containerCenterYPx = containerCenterYPx,
                iconDistancePx = iconDistancePx,
                iconSizePx = iconSizePx,
                primaryColor = primaryColor
            )
        }

        // 主要的FloatingActionButton
        DraggableFab(
            playerState = playerState,
            offsetX = offsetX,
            offsetY = offsetY,
            isDragging = isDragging,
            isAdsorbed = isAdsorbed,
            containerCenterXPx = containerCenterXPx,
            containerCenterYPx = containerCenterYPx,
            fabSizePx = fabSizePx,
            iconDistancePx = iconDistancePx,
            maxDragDistance = maxDragDistance,
            adsorptionThreshold = adsorptionThreshold,
            desorptionDistance = desorptionDistance,
            rotation = rotation,
            coroutineScope = coroutineScope,
            onDragStart = {
                isDragging = true
                showIcons = true
                dragTarget = null
            },
            onDragEnd = {
                isDragging = false
                isAdsorbed = false

                // 根据拖拽目标执行相应操作
                when (dragTarget) {
                    DragTarget.PREVIOUS -> playerViewModel.previous()
                    DragTarget.NEXT -> playerViewModel.next()
                    DragTarget.PLAY_PAUSE -> playerViewModel.isPlaying(!playerState.isPlaying)
                    else -> { /* 没有明确目标，不执行操作 */
                    }
                }

                // 回到中心位置
                coroutineScope.launch {
                    offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                    offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))

                    // 延迟隐藏图标
                    kotlinx.coroutines.delay(300)
                    showIcons = false
                    dragTarget = null
                }
            },
            onAdsorbed = { target ->
                dragTarget = target
                isAdsorbed = true
            }
        )
    }
}

/**
 * 功能图标组件
 */
@Composable
private fun FunctionIcons(
    playerState: PlayerState,
    dragTarget: DragTarget?,
    containerCenterXPx: Float,
    containerCenterYPx: Float,
    iconDistancePx: Float,
    iconSizePx: Float,
    primaryColor: Color
) {
    val density = LocalDensity.current

    // 左侧图标 - 上一首
    Icon(
        imageVector = Icons.Default.FastRewind,
        contentDescription = null,
        tint = primaryColor,
        modifier = Modifier
            .size(32.dp)
            .offset(
                x = with(density) { (containerCenterXPx - iconDistancePx - iconSizePx / 2).toDp() },
                y = with(density) { (containerCenterYPx - iconSizePx / 2).toDp() }
            )
            .zIndex(15f)
            .graphicsLayer {
                alpha = if (dragTarget == DragTarget.PREVIOUS) 1f else 0.7f
            }
    )

    // 右侧图标 - 下一首
    Icon(
        imageVector = Icons.Default.FastForward,
        contentDescription = null,
        tint = primaryColor,
        modifier = Modifier
            .size(32.dp)
            .offset(
                x = with(density) { (containerCenterXPx + iconDistancePx - iconSizePx / 2).toDp() },
                y = with(density) { (containerCenterYPx - iconSizePx / 2).toDp() }
            )
            .zIndex(15f)
            .graphicsLayer {
                alpha = if (dragTarget == DragTarget.NEXT) 1f else 0.7f
            }
    )

    // 顶部图标 - 播放/暂停
    Icon(
        imageVector = Icons.AutoMirrored.Filled.List,
        contentDescription = null,
        tint = primaryColor,
        modifier = Modifier
            .size(32.dp)
            .offset(
                x = with(density) { (containerCenterXPx - iconSizePx / 2).toDp() },
                y = with(density) { (containerCenterYPx - iconDistancePx - iconSizePx / 2).toDp() }
            )
            .zIndex(15f)
            .graphicsLayer {
                alpha = if (dragTarget == DragTarget.OTHER) 1f else 0.7f
            }
    )

    // 底部图标 - 播放/暂停
    Icon(
        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        contentDescription = null,
        tint = primaryColor,
        modifier = Modifier
            .size(32.dp)
            .offset(
                x = with(density) { (containerCenterXPx - iconSizePx / 2).toDp() },
                y = with(density) { (containerCenterYPx + iconDistancePx - iconSizePx / 2).toDp() }
            )
            .zIndex(15f)
            .graphicsLayer {
                alpha = if (dragTarget == DragTarget.PLAY_PAUSE) 1f else 0.7f
            }
    )
}

/**
 * 可拖拽的FAB组件
 */
@Composable
private fun DraggableFab(
    playerState: PlayerState,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    isDragging: Boolean,
    isAdsorbed: Boolean,
    containerCenterXPx: Float,
    containerCenterYPx: Float,
    fabSizePx: Float,
    iconDistancePx: Float,
    maxDragDistance: Float,
    adsorptionThreshold: Float,
    desorptionDistance: Float,
    rotation: Float,
    coroutineScope: CoroutineScope,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onAdsorbed: (DragTarget) -> Unit
) {
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .size(with(density) { fabSizePx.toDp() })
            .offset(
                x = with(density) { (containerCenterXPx - fabSizePx / 2).toDp() },
                y = with(density) { (containerCenterYPx - fabSizePx / 2).toDp() }
            )
            .offset {
                IntOffset(
                    offsetX.value.roundToInt(),
                    offsetY.value.roundToInt()
                )
            }
            .shadow(
                elevation = if (isDragging) 16.dp else 8.dp,
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        // 如果已经吸附，则需要拖拽足够远才能解除吸附
                        if (isAdsorbed) {
                            val dragDistance = sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                            if (dragDistance < desorptionDistance) {
                                return@detectDragGestures
                            }
                        }

                        // 计算新的偏移位置
                        val newOffsetX = offsetX.value + dragAmount.x
                        val newOffsetY = offsetY.value + dragAmount.y

                        // 限制最大拖拽距离
                        val distanceFromCenter = sqrt(newOffsetX * newOffsetX + newOffsetY * newOffsetY)
                        if (distanceFromCenter > maxDragDistance) {
                            val ratio = maxDragDistance / distanceFromCenter
                            coroutineScope.launch {
                                offsetX.snapTo(newOffsetX * ratio)
                                offsetY.snapTo(newOffsetY * ratio)
                            }
                        } else {
                            coroutineScope.launch {
                                offsetX.snapTo(newOffsetX)
                                offsetY.snapTo(newOffsetY)
                            }
                        }

                        // 定义目标吸附位置（图标中心位置）- 使用像素单位
                        val targets = listOf(
                            DragTargetPosition(DragTarget.PREVIOUS, -iconDistancePx, 0f),
                            DragTargetPosition(DragTarget.NEXT, iconDistancePx, 0f),
                            DragTargetPosition(DragTarget.OTHER, 0f, -iconDistancePx),
                            DragTargetPosition(DragTarget.PLAY_PAUSE, 0f, iconDistancePx)
                        )

                        // 检查是否接近某个目标点
                        var closestTarget: DragTarget? = null
                        var minDistance = Float.MAX_VALUE

                        targets.forEach { target ->
                            val distance = sqrt(
                                (newOffsetX - target.x) * (newOffsetX - target.x) +
                                        (newOffsetY - target.y) * (newOffsetY - target.y)
                            )
                            if (distance < minDistance) {
                                minDistance = distance
                                closestTarget = target.target
                            }
                        }

                        // 如果足够接近某个目标，则吸附到该位置（仅在未吸附时）
                        if (minDistance <= adsorptionThreshold && !isAdsorbed) {
                            closestTarget?.let { onAdsorbed(it) }

                            val targetPosition = targets.find { it.target == closestTarget }
                            coroutineScope.launch {
                                targetPosition?.let {
                                    offsetX.animateTo(it.x, spring(stiffness = Spring.StiffnessMediumLow))
                                    offsetY.animateTo(it.y, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
            .clip(CircleShape)
            .graphicsLayer {
                rotationZ = if (playerState.isPlaying) rotation else 0f
                scaleX = if (isDragging) 1.1f else 1.0f
                scaleY = if (isDragging) 1.1f else 1.0f
            },
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .fillMaxSize()
        ) {
            // 背景图片
            Image(
                painter = rememberAsyncImagePainter(
                    playerState.track?.album?.image?.orEmpty()
                ),
                contentDescription = "Now Playing Album Art",
                modifier = Modifier
                    .size(with(density) { fabSizePx.toDp() })
                    .clip(CircleShape)
                    .rotate(if (playerState.isPlaying) rotation else 0f),
                contentScale = ContentScale.Crop
            )

            // 播放状态指示器
            if (playerState.isPlaying) {
                Box(
                    modifier = Modifier
                        .size(with(density) { fabSizePx.toDp() })
                        .clip(CircleShape)
                        .graphicsLayer {
                            alpha = 0.4f
                        }
                )
            }

            // 进度环
            Canvas(
                modifier = Modifier
                    .size(with(density) { fabSizePx.toDp() })
                    .fillMaxSize()
            ) {
                // 背景环
                drawArc(
                    color = Color.White.copy(alpha = 0.3f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    size = Size(this.size.width - 8.dp.toPx(), this.size.height - 8.dp.toPx()),
                    style = Stroke(width = 4f),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx())
                )

                // 进度环
                val progress = playerState.duration?.toPercent(playerState.position) ?: 0f
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    size = Size(this.size.width - 8.dp.toPx(), this.size.height - 8.dp.toPx()),
                    style = Stroke(width = 4f),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}

// 拖拽目标位置数据类
data class DragTargetPosition(val target: DragTarget, val x: Float, val y: Float)

// 拖拽目标枚举
enum class DragTarget {
    PREVIOUS,    // 上一首
    NEXT,        // 下一首
    PLAY_PAUSE,   // 播放/暂停
    OTHER,       // 其他
}
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
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
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
    val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()
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

        DraggableFab(
            playerState = playerState,
            offsetX = offsetX,
            offsetY = offsetY,
            isDragging = isDragging,
            containerCenterXPx = containerCenterXPx,
            containerCenterYPx = containerCenterYPx,
            fabSizePx = fabSizePx,
            iconDistancePx = iconDistancePx,
            maxDragDistance = maxDragDistance,
            rotation = rotation,
            coroutineScope = coroutineScope,
            onDragStart = {
                isDragging = true
                showIcons = true
                dragTarget = null
            },
            onDragEnd = { target ->
                isDragging = false
                dragTarget = target

                // 根据拖拽目标执行相应操作
                when (dragTarget) {
                    DragTarget.PREVIOUS -> playerViewModel.previous()
                    DragTarget.NEXT -> playerViewModel.next()
                    DragTarget.PLAY_PAUSE -> playerViewModel.isPlaying(!playerState.isPlaying)
                    DragTarget.PLAYER -> defaultLayoutViewModel.updateFullScreenPlayer(true)
                    else -> { /* 没有明确目标，不执行操作 */
                    }
                }

                // 回到中心位置
                coroutineScope.launch {
                    // 根据拖拽距离决定回归顺序，使动画更自然
                    val distanceX = kotlin.math.abs(offsetX.value)
                    val distanceY = kotlin.math.abs(offsetY.value)
                    
                    if (distanceX > distanceY) {
                        offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                    } else {
                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                    }

                    // 延迟隐藏图标
                    kotlinx.coroutines.delay(600)
                    showIcons = false
                    dragTarget = null
                }
            }
        )


        if (isDragging || showIcons) {
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
    val backgroundColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

    // 左侧图标 - 上一首
    Box(
        modifier = Modifier
            .size(48.dp)
            .offset(
                x = with(density) { (containerCenterXPx - iconDistancePx - 24.dp.toPx()).toDp() },
                y = with(density) { (containerCenterYPx - 24.dp.toPx()).toDp() }
            )
            .zIndex(14f)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.9f))
    )
    Icon(
        imageVector = Icons.Default.FastRewind,
        contentDescription = null,
        tint = contentColor,
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
    Box(
        modifier = Modifier
            .size(48.dp)
            .offset(
                x = with(density) { (containerCenterXPx + iconDistancePx - 24.dp.toPx()).toDp() },
                y = with(density) { (containerCenterYPx - 24.dp.toPx()).toDp() }
            )
            .zIndex(14f)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.9f))
    )
    Icon(
        imageVector = Icons.Default.FastForward,
        contentDescription = null,
        tint = contentColor,
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

    // 顶部图标 - 播放界面
    Box(
        modifier = Modifier
            .size(48.dp)
            .offset(
                x = with(density) { (containerCenterXPx - 24.dp.toPx()).toDp() },
                y = with(density) { (containerCenterYPx - iconDistancePx - 24.dp.toPx()).toDp() }
            )
            .zIndex(14f)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.9f))
    )
    Icon(
        imageVector = Icons.AutoMirrored.Filled.List,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier
            .size(32.dp)
            .offset(
                x = with(density) { (containerCenterXPx - iconSizePx / 2).toDp() },
                y = with(density) { (containerCenterYPx - iconDistancePx - iconSizePx / 2).toDp() }
            )
            .zIndex(15f)
            .graphicsLayer {
                alpha = if (dragTarget == DragTarget.PLAYER) 1f else 0.7f
            }
    )

    // 底部图标 - 播放/暂停
    Box(
        modifier = Modifier
            .size(48.dp)
            .offset(
                x = with(density) { (containerCenterXPx - 24.dp.toPx()).toDp() },
                y = with(density) { (containerCenterYPx + iconDistancePx - 24.dp.toPx()).toDp() }
            )
            .zIndex(14f)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.9f))
    )
    Icon(
        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        contentDescription = null,
        tint = contentColor,
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
    containerCenterXPx: Float,
    containerCenterYPx: Float,
    fabSizePx: Float,
    iconDistancePx: Float,
    maxDragDistance: Float,
    rotation: Float,
    coroutineScope: CoroutineScope,
    onDragStart: () -> Unit,
    onDragEnd: (DragTarget?) -> Unit,
) {
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary
    var newOffsetX: Float by remember { mutableFloatStateOf(0f) }
    var newOffsetY: Float by remember { mutableFloatStateOf(0f) }
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

                        // 计算新的偏移位置
                        newOffsetX = offsetX.value + dragAmount.x
                        newOffsetY = offsetY.value + dragAmount.y

                        // 正常更新位置
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
                    },
                    onDragEnd = {
                        // 定义目标吸附位置（图标中心位置）- 使用像素单位
                        val targets = mapOf(
                            DragTarget.PREVIOUS to Pair(-iconDistancePx, 0f),
                            DragTarget.NEXT to Pair(iconDistancePx, 0f),
                            DragTarget.PLAYER to Pair(0f, -iconDistancePx),
                            DragTarget.PLAY_PAUSE to Pair(0f, iconDistancePx),
                        )

                        // 检查是否接近某个目标点
                        var closestTarget: DragTarget? = null
                        var minDistance = Float.MAX_VALUE

                        targets.forEach { (key, value) ->
                            val (x, y) = value
                            val distance = sqrt(
                                (newOffsetX - x) * (newOffsetX - x) +
                                        (newOffsetY - y) * (newOffsetY - y)
                            )
                            if (distance < minDistance) {
                                minDistance = distance
                                closestTarget = key
                            }
                        }

                        onDragEnd(closestTarget)
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

// 拖拽目标枚举
enum class DragTarget {
    PREVIOUS,    // 上一首
    NEXT,        // 下一首
    PLAY_PAUSE,   // 播放/暂停
    PLAYER,      // 播放界面
}
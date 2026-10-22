package com.eimsound.eimusic.ui.input

import kotlin.math.abs
import kotlin.math.exp

/**
 * 滚轮惯性状态：位置以“行”为单位，速度按 e^(-friction*dt) 衰减。
 *
 * 纯逻辑，不依赖 Compose，方便单测，后续也可以整体下沉到 Mosaic。
 */
class MomentumScroll(
    private val friction: Float = DEFAULT_FRICTION,
    private val maxSpeed: Float = DEFAULT_MAX_SPEED,
    private val minSpeed: Float = DEFAULT_MIN_SPEED,
) {
    var position: Float = 0f
        private set

    var velocity: Float = 0f
        private set

    val isMoving: Boolean get() = velocity != 0f

    /** 键盘/点击等直接定位，并清空惯性。 */
    fun jumpTo(index: Int) {
        position = index.toFloat()
        velocity = 0f
    }

    /** 一次滚轮脉冲：rows 为负向上、正向下。 */
    fun impulse(rows: Int) {
        velocity = (velocity + rows * friction).coerceIn(-maxSpeed, maxSpeed)
    }

    /**
     * 推进 [dt] 秒，返回是否仍在移动。
     * 位置限制在 [0, maxPosition]，撞到边界时速度清零。
     */
    fun advance(dt: Float, maxPosition: Float): Boolean {
        if (velocity == 0f) return false
        val step = dt.coerceIn(0f, MAX_FRAME_DT)
        var next = position + velocity * step
        if (next <= 0f) {
            next = 0f
            velocity = 0f
        } else if (next >= maxPosition) {
            next = maxPosition
            velocity = 0f
        }
        position = next
        if (velocity != 0f) {
            velocity *= exp(-friction * step)
            if (abs(velocity) < minSpeed) velocity = 0f
        }
        return velocity != 0f
    }

    companion object {
        /** 摩擦系数（1/s）：越大停得越快；一次滚轮的位移约等于 velocity / friction。 */
        const val DEFAULT_FRICTION = 3f
        const val DEFAULT_MAX_SPEED = 60f
        const val DEFAULT_MIN_SPEED = 0.5f
        private const val MAX_FRAME_DT = 0.05f
    }
}

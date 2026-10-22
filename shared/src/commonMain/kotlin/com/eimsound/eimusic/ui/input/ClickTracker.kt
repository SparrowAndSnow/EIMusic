package com.eimsound.eimusic.ui.input

/**
 * 单击/双击判定：同一目标在 [doubleClickMillis] 内再次按下算双击。
 * 纯逻辑，不依赖 Compose。
 */
class ClickTracker(
    private val doubleClickMillis: Long = DEFAULT_DOUBLE_CLICK_MILLIS,
) {
    private var lastIndex: Int = -1
    private var lastPressedAt: Long = 0L

    fun press(index: Int, nowMillis: Long): Boolean {
        val doubleClick = index == lastIndex && nowMillis - lastPressedAt in 0L..doubleClickMillis
        lastIndex = index
        lastPressedAt = nowMillis
        return doubleClick
    }

    fun reset() {
        lastIndex = -1
        lastPressedAt = 0L
    }

    companion object {
        const val DEFAULT_DOUBLE_CLICK_MILLIS = 400L
    }
}

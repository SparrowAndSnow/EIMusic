package com.eimsound.eimusic.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 窗口拖拽条组件
 * 在桌面端提供窗口拖拽功能，在其他平台提供空实现
 */
@Composable
expect fun WindowDragHandle(
    modifier: Modifier = Modifier
)
package com.eimsound.eimusic.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.components.PlayingList


enum class SidebarComponent {
    PLAYLIST,
}

@Composable
fun SideBar(component: SidebarComponent) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxSize()
    ) {
        AnimatedContent(targetState = component) { target ->
            when(target) {
                SidebarComponent.PLAYLIST -> PlayingList()
            }
        }
    }
}

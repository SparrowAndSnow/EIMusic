package com.eimsound.eimusic.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.components.PlayingList
import com.eimsound.eimusic.theme.LocalRadii
import com.eimsound.eimusic.util.onBackHandler

enum class SidebarComponent {
    PLAYLIST,
}

@Composable
fun SideBar(component: SidebarComponent, onDismiss: () -> Unit) {
    val radii = LocalRadii.current
    onBackHandler(isBackEnabled = true){
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .clip(radii.medium)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        AnimatedContent(
            targetState = component,
            modifier = Modifier
                .fillMaxSize()
        ) { target ->
            when (target) {
                SidebarComponent.PLAYLIST -> PlayingList()
            }
        }
    }
}

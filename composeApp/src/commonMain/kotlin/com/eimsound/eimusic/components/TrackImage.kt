package com.eimsound.eimusic.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter

@Composable
fun TrackImage(
    modifier: Modifier = Modifier,
    image: String?,
    isPlaying: Boolean = false,
    onPlayClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val painter = rememberAsyncImagePainter(image)
    Box(
        modifier = modifier.clip(RoundedCornerShape(4.dp))
            .width(64.dp).height(64.dp)
            .hoverable(interactionSource)
            .clickable {
                onPlayClick()
            }) {
        Image(
            painter,
            image,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).width(64.dp).height(64.dp),
            contentScale = ContentScale.Crop
        )

        if (isPlaying) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))) {
                Icon(
                    modifier = Modifier.align(Alignment.Center),
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else if (isHovered) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
            }
        }

    }
}
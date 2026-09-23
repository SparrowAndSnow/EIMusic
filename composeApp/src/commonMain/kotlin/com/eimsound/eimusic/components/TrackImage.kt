package com.eimsound.eimusic.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.eimsound.eimusic.theme.LocalPalette
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.graphic_eq_24px
import eimusic.composeapp.generated.resources.play_arrow_fill_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun TrackImage(
    modifier: Modifier = Modifier,
    image: String?,
    isPlaying: Boolean = false,
    onPlayClick: () -> Unit = {}
) {
    val palette = LocalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onPlayClick)
    ) {
        if(image == null){
            Box(
                modifier = Modifier.fillMaxSize().background(palette.surfaceRaised),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.graphic_eq_24px),
                    contentDescription = null,
                    tint = palette.textPrimary.copy(alpha = 0.5f),
                )
            }
        }else{
            Image(
                painter = rememberAsyncImagePainter(image),
                contentDescription = image,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }


        val showOverlay = isPlaying || isHovered
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = if (isPlaying) painterResource(Res.drawable.graphic_eq_24px) else painterResource(Res.drawable.play_arrow_fill_24px),
                    contentDescription = null,
                    tint = palette.accent,
                )
            }
        }
    }
}

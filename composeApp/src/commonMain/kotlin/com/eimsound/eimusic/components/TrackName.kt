package com.eimsound.eimusic.components

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TrackName(modifier: Modifier = Modifier, name: String, onClick: () -> Unit) {
    LinkText(
        text = name,
        modifier = modifier
            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
            .clickable {
                onClick()
            }
    )
}
package com.eimsound.eimusic.components

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eimsound.eimusic.music.Artist

@Composable
fun ArtistList(
    modifier: Modifier = Modifier,
    artists: List<Artist>,
    onClick: (Artist) -> Unit = {},
    divider: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
    ) {
        artists.forEachIndexed{ index, it ->
            LinkText(
                text = it?.name.orEmpty(),
                modifier = Modifier.clickable {
                    onClick(it)
                }
            )
            if (index != artists.lastIndex) {
                divider()
            }
        }
    }
}
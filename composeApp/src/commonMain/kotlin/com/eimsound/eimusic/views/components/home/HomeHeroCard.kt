package com.eimsound.eimusic.views.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii
import com.eimsound.eimusic.views.components.RefreshButton

@Composable
fun HomeHeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    isLoading: Boolean = false
) {
    val palette = LocalPalette.current
    val radii = LocalRadii.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.33f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "EIMUSIC MIX",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live",
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent
            )
            RefreshButton(onRefresh,!isLoading)
        }
    }
}

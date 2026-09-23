package com.eimsound.eimusic.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.theme.LocalPalette
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.refresh
import org.jetbrains.compose.resources.stringResource

@Composable
fun RefreshButton(onRefresh: () -> Unit, enabled: Boolean = true, backgroundColor: Color? = null) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) {
                onRefresh()
            }
            .background(
                color = backgroundColor ?: palette.accent,
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(Res.string.refresh),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
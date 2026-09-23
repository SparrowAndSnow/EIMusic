package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_preferences
import eimusic.composeapp.generated.resources.setting_preferences_desc
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingHeader() {
    val palette = LocalPalette.current
    val radii = LocalRadii.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = radii.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.setting_preferences),
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary
            )
            Text(
                text = stringResource(Res.string.setting_preferences_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
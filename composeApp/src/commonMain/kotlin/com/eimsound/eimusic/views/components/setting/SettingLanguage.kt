package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eimsound.eimusic.viewmodel.LanguageState
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_language
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingLanguage(languageState: LanguageState, setLanguage: (String) -> Unit) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    SettingSection(title = stringResource(Res.string.setting_language)) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_language)) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            supportingContent = {
                Text(if (languageState.language == "zh") "中文" else "English")
            },
            modifier = Modifier.clickable { showLanguageDialog = true }
        )
    }

    LanguageDialog(
        isVisible = showLanguageDialog,
        onDismiss = { showLanguageDialog = false },
        setLanguage = setLanguage,
        currentLanguage = languageState.language
    )
}
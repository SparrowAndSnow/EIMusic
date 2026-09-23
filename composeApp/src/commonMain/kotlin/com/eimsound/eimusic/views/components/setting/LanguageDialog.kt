package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_about_confirm
import eimusic.composeapp.generated.resources.setting_language
import eimusic.composeapp.generated.resources.setting_language_cancel
import org.jetbrains.compose.resources.stringResource

/**
 * 语言选择对话框组件
 */
@Composable
fun LanguageDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    setLanguage: (String) ->  Unit,
    currentLanguage: String
) {
    if (!isVisible) return
    
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    
    val languages = mapOf(
        "zh" to "中文",
        "en" to "English"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(Res.string.setting_language),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                languages.forEach { (code, name) ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedLanguage == code)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selectedLanguage == code,
                                onClick = {
                                    selectedLanguage = code
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        },
                        modifier = Modifier
                            .clickable {
                                selectedLanguage = code
                            }
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    setLanguage(selectedLanguage)
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(Res.string.setting_about_confirm),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(Res.string.setting_language_cancel),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eimsound.eimusic.version.UpdateInfo
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_update_current_version
import eimusic.composeapp.generated.resources.setting_update_download
import eimusic.composeapp.generated.resources.setting_update_install_now
import eimusic.composeapp.generated.resources.setting_update_latest_version
import eimusic.composeapp.generated.resources.setting_update_later
import eimusic.composeapp.generated.resources.update_dialog_critical
import eimusic.composeapp.generated.resources.update_dialog_description
import eimusic.composeapp.generated.resources.update_dialog_dismiss
import eimusic.composeapp.generated.resources.update_dialog_new_version_found
import eimusic.composeapp.generated.resources.update_dialog_release_date
import eimusic.composeapp.generated.resources.update_dialog_release_notes
import eimusic.composeapp.generated.resources.update_dialog_title
import org.jetbrains.compose.resources.stringResource

/**
 * 更新提示对话框
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    currentVersion: String,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!updateInfo.isCritical) onDismiss() },
        properties = DialogProperties(
            dismissOnClickOutside = !updateInfo.isCritical,
            dismissOnBackPress = !updateInfo.isCritical
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 24.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header with icon and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.update_dialog_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (updateInfo.isCritical) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(stringResource(Res.string.update_dialog_critical))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    labelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                    
                    IconButton(onClick = { if (!updateInfo.isCritical) onDismiss() }) {
                        Text("✕")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Version info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(Res.string.setting_update_current_version),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentVersion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(Res.string.setting_update_latest_version),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = updateInfo.version,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${stringResource(Res.string.update_dialog_release_date)}：${updateInfo.releaseDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Update description
                Text(
                    text = updateInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = updateInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Release notes
                if (!updateInfo.releaseNotes.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.update_dialog_release_notes),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                text = updateInfo.releaseNotes ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!updateInfo.isCritical) {
                        OutlinedButton(
                            onClick = onLater,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(Res.string.setting_update_later))
                        }
                    }
                    
                    Button(
                        onClick = onUpdate,
                        modifier = Modifier
                            .weight(if (updateInfo.isCritical) 1f else 1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            if (updateInfo.isCritical) 
                                stringResource(Res.string.setting_update_install_now)
                            else 
                                stringResource(Res.string.setting_update_download)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 简化的更新信息提示框 (Snackbar 风格)
 */
@Composable
fun UpdateSnackbar(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "✨ ${stringResource(Res.string.update_dialog_new_version_found)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "v${updateInfo.version} - ${updateInfo.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.update_dialog_dismiss))
                }
                
                Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(stringResource(Res.string.update_dialog_description))
                }
            }
        }
    }
}

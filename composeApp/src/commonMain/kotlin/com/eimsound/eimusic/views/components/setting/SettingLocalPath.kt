package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.eimsound.eimusic.viewmodel.LocalPathsState
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.add_24px
import eimusic.composeapp.generated.resources.setting_add_local_path
import eimusic.composeapp.generated.resources.setting_local
import eimusic.composeapp.generated.resources.setting_local_paths
import eimusic.composeapp.generated.resources.setting_tap_to_remove
import io.github.vinceglb.filekit.PlatformFile
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingLocalPath(
    localPathsState: LocalPathsState,
    openDirectoryPicker: ((PlatformFile?) -> Unit) -> Unit,
    addLocalPath: (String) -> Unit,
    removeLocalPath: (String) -> Unit
) {
    var newPath by remember { mutableStateOf("") }
    var showAddPathDialog by remember { mutableStateOf(false) }

    SettingSection(title = stringResource(Res.string.setting_local)) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_local_paths)) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            trailingContent = {
                IconButton(onClick = { showAddPathDialog = true }) {
                    Icon(
                        painterResource(Res.drawable.add_24px),
                        contentDescription = stringResource(Res.string.setting_add_local_path)
                    )
                }
            }
        )
        localPathsState.paths.forEach { path ->
            ListItem(
                headlineContent = { Text(path) },
                supportingContent = { Text(stringResource(Res.string.setting_tap_to_remove)) },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
                modifier = Modifier.clickable { removeLocalPath(path) }
            )
        }
    }

    AddLocalPathDialog(
        isVisible = showAddPathDialog,
        newPath = newPath,
        onNewPathChange = { newPath = it },
        onDirectoryPickerClick = {
            openDirectoryPicker {
                newPath = it.toString()
            }
        },
        onDismiss = { showAddPathDialog = false },
        onConfirm = {
            if (newPath.isNotBlank()) {
                addLocalPath(newPath.trim())
            }
            newPath = ""
            showAddPathDialog = false
        }
    )
}
package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.music.TrackSource
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.add_24px
import eimusic.composeapp.generated.resources.cancel
import eimusic.composeapp.generated.resources.check_circle_fill_24px
import eimusic.composeapp.generated.resources.close_24px
import eimusic.composeapp.generated.resources.config_sources
import eimusic.composeapp.generated.resources.config_sources_add
import eimusic.composeapp.generated.resources.config_sources_empty
import eimusic.composeapp.generated.resources.config_sources_remove
import eimusic.composeapp.generated.resources.config_sources_url_hint
import eimusic.composeapp.generated.resources.setting_save
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SourceListDialog(
    isVisible: Boolean,
    sources: Map<TrackSource, List<String>>,
    onSourcesChange: (Map<TrackSource, List<String>>) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!isVisible) return

    var selectedSource by remember { mutableStateOf(TrackSource.NETEASE) }
    var newSource by remember { mutableStateOf("") }
    val currentUrls = sources[selectedSource].orEmpty()

    // LOCAL / UNKNOWN 无源 URL 概念，排除
    val editableSources = remember {
        TrackSource.entries.filter { it != TrackSource.LOCAL && it != TrackSource.UNKNOWN }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.config_sources)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 分类选择：FilterChip 换行排列，选中高亮 + 对勾
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    editableSources.forEach { source ->
                        FilterChip(
                            selected = source == selectedSource,
                            onClick = {
                                selectedSource = source
                                newSource = ""
                            },
                            label = { Text(source.displayLabel()) },
                            leadingIcon = if (source == selectedSource) {
                                { Icon(painterResource(Res.drawable.check_circle_fill_24px), contentDescription = null) }
                            } else null
                        )
                    }
                }

                // 当前分类的源地址列表
                if (currentUrls.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.config_sources_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedCard {
                        Column {
                            currentUrls.forEachIndexed { index, url ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = url,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = {
                                        onSourcesChange(sources + (selectedSource to (currentUrls - url)))
                                    }) {
                                        Icon(
                                            painterResource(Res.drawable.close_24px),
                                            contentDescription = stringResource(Res.string.config_sources_remove)
                                        )
                                    }
                                }
                                if (index != currentUrls.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                                }
                            }
                        }
                    }
                }

                // 添加新源地址
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSource,
                        onValueChange = { newSource = it },
                        label = { Text(stringResource(Res.string.config_sources_add)) },
                        placeholder = { Text(stringResource(Res.string.config_sources_url_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            val trimmed = newSource.trim()
                            if (trimmed.isNotEmpty() && trimmed !in currentUrls) {
                                onSourcesChange(sources + (selectedSource to (currentUrls + trimmed)))
                            }
                            newSource = ""
                        }
                    ) {
                        Icon(painterResource(Res.drawable.add_24px), contentDescription = stringResource(Res.string.config_sources_add))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.setting_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

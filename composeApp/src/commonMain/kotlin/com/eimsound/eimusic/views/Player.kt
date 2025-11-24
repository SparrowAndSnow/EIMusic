package com.eimsound.eimusic.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.components.*
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FullScreenPlayer(
    onDismiss: () -> Unit
) {
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val playerState by playerViewModel.state.collectAsState()
    // 全屏播放界面
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box {
            // 顶部关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .zIndex(1f), // 确保关闭按钮在最上层
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // 内容区域，防止点击关闭
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .clickable(enabled = false) { } // 阻止点击事件传递到背景，但不禁用按钮
            ) {
                
                // 专辑封面和歌曲信息
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    // 专辑封面
                    TrackImage(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        image = playerState.track?.album?.image,
                        isPlaying = playerState.isPlaying
                    ) { 
                        playerViewModel.isPlaying(!playerState.isPlaying)
                    }

                    // 歌曲信息
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = playerState.track?.name ?: "未知歌曲",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = playerState.track?.artists?.joinToString(", ") { it.name ?: "" } ?: "未知艺术家",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 进度条
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PlayerSlider(
                            value = playerState.duration?.toPercent(playerState.position) ?: 0f,
                            bufferValue = playerState.bufferProgress,
                            showBufferProgress = playerState.track != null,
                            onValueChangeFinished = { progress ->
                                playerState.duration?.let { duration ->
                                    playerViewModel.seek(duration.percentOf(progress))
                                }
                            },
                            onValueChange = { }
                        )
                        
                        TimeDisplay(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            position = playerState.position,
                            duration = playerState.duration ?: Duration(0),
                            isDragging = false,
                            draggingPosition = Duration(0)
                        )
                    }
                }

                // 控制按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 播放模式
                    PlayModeButton(
                        playMode = playerState.playMode,
                        onPlayModeChanged = playerViewModel::onPlayModeChanged
                    )
                    
                    // 上一首
                    IconButton(
                        onClick = playerViewModel::previous,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "上一首"
                        )
                    }
                    
                    // 播放/暂停
                    PlayPauseButton(
                        isPlaying = playerState.isPlaying,
                        onIsPlayingChanged = playerViewModel::isPlaying,
                        modifier = Modifier.size(64.dp).padding(horizontal = 4.dp),
                    )
                    
                    // 下一首
                    IconButton(
                        onClick = playerViewModel::next,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "下一首"
                        )
                    }
                    // 音量控制
                    Volume(
                        volume = playerState.volume,
                        isMute = playerState.isMute,
                        onVolumeChanged = playerViewModel::onVolumeChanged,
                        onIsMuteChanged = playerViewModel::onIsMuteChanged
                    )
                }
            }
        }
    }
}


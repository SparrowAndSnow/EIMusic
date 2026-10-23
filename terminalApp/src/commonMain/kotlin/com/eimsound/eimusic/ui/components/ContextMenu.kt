package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.clickable
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Popup
import com.jakewharton.mosaic.ui.Text

data class TrackActionHost(
    val showMenu: (title: String, items: List<ContextMenuItem>) -> Unit,
    val showInfo: (Track) -> Unit,
)

val LocalTrackActionHost = staticCompositionLocalOf<TrackActionHost?> { null }

data class ContextMenuItem(
    val label: String,
    val onClick: () -> Unit,
)

@Stable
class ContextMenuState {
    var title by mutableStateOf("")
        private set
    var items by mutableStateOf<List<ContextMenuItem>>(emptyList())
        private set

    val isOpen: Boolean get() = items.isNotEmpty()

    fun show(title: String, items: List<ContextMenuItem>) {
        this.title = title
        this.items = items
    }

    fun dismiss() {
        items = emptyList()
    }
}

@Composable
fun rememberContextMenuState(): ContextMenuState = remember { ContextMenuState() }

/** 通用右键菜单：鼠标单击或 ↑/↓ + Enter。 */
@Composable
fun ContextMenu(state: ContextMenuState) {
    if (!state.isOpen) return
    val palette = LocalTuiColors.current
    var highlighted by remember(state.items) { mutableIntStateOf(0) }

    fun activate(index: Int) {
        val item = state.items.getOrNull(index) ?: return
        state.dismiss()
        item.onClick()
    }

    Popup(
        modal = true,
        alignment = com.jakewharton.mosaic.ui.Alignment.Center,
        onDismissRequest = state::dismiss,
    ) {
        DialogPanel(title = state.title, onDismiss = state::dismiss) {
            Column(
                modifier = Modifier
                    .padding(2)
                    .onKeyEvent { event ->
                        when (event.key) {
                            "ArrowDown", "j" -> {
                                highlighted = (highlighted + 1).coerceAtMost(state.items.lastIndex)
                                true
                            }
                            "ArrowUp", "k" -> {
                                highlighted = (highlighted - 1).coerceAtLeast(0)
                                true
                            }
                            "Enter" -> {
                                activate(highlighted)
                                true
                            }
                            "Escape" -> {
                                state.dismiss()
                                true
                            }
                            else -> false
                        }
                    },
            ) {
                state.items.forEachIndexed { index, item ->
                    Text(
                        (if (index == highlighted) " ▸ " else "   ") + item.label,
                        modifier = Modifier.clickable { activate(index) },
                        color = if (index == highlighted) palette.highlightFg else palette.textFg,
                    )
                }
            }
        }
    }
}

/** 曲目通用操作项；列表额外操作追加在 [extra] 前面。 */
fun trackContextItems(
    onAddToPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onShowInfo: () -> Unit,
    extra: List<ContextMenuItem> = emptyList(),
): List<ContextMenuItem> = buildList {
    add(ContextMenuItem("Add to playlist", onAddToPlaylist))
    add(ContextMenuItem("Play next", onPlayNext))
    add(ContextMenuItem("Track info", onShowInfo))
    addAll(extra)
}

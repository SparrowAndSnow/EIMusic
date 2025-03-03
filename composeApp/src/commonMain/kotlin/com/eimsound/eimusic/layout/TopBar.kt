package com.eimsound.eimusic.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    val textFieldState = rememberTextFieldState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        SearchBar(
            colors = SearchBarColors(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            ),
            inputField = {
                SearchBarDefaults.InputField(
                    onSearch = { expanded = false },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Hinted search text") },
                    leadingIcon = {
                        if (expanded) {
                            IconButton(
                                modifier = Modifier.pointerHoverIcon(
                                    PointerIcon.Companion.Default
                                ), onClick = {
                                    expanded = false
                                }) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                            }
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    trailingIcon = {
                        if (expanded) {
                            IconButton(
                                modifier = Modifier.pointerHoverIcon(
                                    PointerIcon.Companion.Default
                                ), onClick = {
                                    textFieldState.clearText()
                                }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    query = textFieldState.text.toString(),
                    onQueryChange = { textFieldState.setTextAndPlaceCursorAtEnd(it) }
                )
            },
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            }) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                repeat(4) { idx ->
                    val resultText = "Suggestion $idx"
                    ListItem(
                        headlineContent = { Text(resultText) },
                        supportingContent = { Text("Additional info") },
                        leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier =
                            Modifier.clickable {
                                textFieldState.setTextAndPlaceCursorAtEnd(resultText)
                                expanded = false
                            }
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

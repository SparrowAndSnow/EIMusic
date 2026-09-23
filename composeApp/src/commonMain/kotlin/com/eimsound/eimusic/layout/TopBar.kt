package com.eimsound.eimusic.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.search_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun TopBar(onSearch: (String) -> Unit) {
    val palette = LocalPalette.current
    val radii = LocalRadii.current
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (query.isNotBlank()) {
                        onSearch(query.trim())
                        focusManager.clearFocus()
                    }
                }
            ),
            modifier = Modifier
                .weight(1f).height(48.dp),
            textStyle = TextStyle(
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            ),
            shape = radii.large,
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.search_24px),
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            },
            placeholder = {
                Text(
                    text = "Search tracks, artists, albums",
                    color = palette.textSecondary,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.borderStrong,
                unfocusedBorderColor = palette.borderSoft,
                focusedContainerColor = palette.surfaceGlass,
                unfocusedContainerColor = palette.surfaceGlass,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
            )
        )
    }
}

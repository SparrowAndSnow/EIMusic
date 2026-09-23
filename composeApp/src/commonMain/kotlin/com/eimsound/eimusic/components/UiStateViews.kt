package com.eimsound.eimusic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.util.LocalToastMessage

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingStateView(text: String) {
    val palette = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingIndicator(color = palette.accent)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun MessageStateView(title: String, description: String) {
    val palette = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


@Composable
fun ToastView(modifier: Modifier = Modifier) {
    val messageManager = LocalToastMessage.current
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = LocalPalette.current
    LaunchedEffect(Unit) {
        messageManager.messages.collect { msg ->
            val result = snackbarHostState.showSnackbar(
                msg.message, msg.actionLabel,
                msg.dismissAction, msg.duration
            )
            if (result == SnackbarResult.ActionPerformed) {
                msg.action?.invoke()
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState, modifier) {
        Snackbar(
            snackbarData = it,
            contentColor = palette.textPrimary,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(8.dp)
        )
    }
}
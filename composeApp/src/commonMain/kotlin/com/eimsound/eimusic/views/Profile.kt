package com.eimsound.eimusic.views

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.profile_title

@Serializable
object ProfileRoute

@Composable
fun ProfileView() {
    Text(stringResource(Res.string.profile_title))
}
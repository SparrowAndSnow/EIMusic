package com.eimsound.eimusic.views

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

@Serializable
object ProfileRoute

@Composable
fun ProfileView() {
    Text("Profile")
}

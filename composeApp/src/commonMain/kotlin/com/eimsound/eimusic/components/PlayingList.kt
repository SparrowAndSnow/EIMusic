package com.eimsound.eimusic.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.eimsound.eimusic.network.models.topfiftycharts.Item

@Composable
fun PlayingList(trackList: List<Item>) {
    LazyColumn {
        items(trackList.size) { index ->
            Text(trackList[index].track?.name ?: "")
        }
    }
}
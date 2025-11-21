package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.components.ListDetail

@Composable
fun MyView() {
    ListDetail(shortStrings)
//    Text(stringResource(Res.string.profile_title))
}


val shortStrings = listOf(
    "Cupcake",
    "Donut",
    "Eclair",
    "Froyo",
    "Gingerbread",
    "Honeycomb",
    "Ice cream sandwich",
    "Jelly bean",
    "Kitkat",
    "Lollipop",
    "Marshmallow",
    "Nougat",
    "Oreo",
    "Pie",
)
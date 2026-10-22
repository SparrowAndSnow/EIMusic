package com.eimsound.eimusic.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.jakewharton.mosaic.ui.Color

val LocalTuiColors: ProvidableCompositionLocal<TuiColors> =
    staticCompositionLocalOf { TuiColors.dark() }

enum class ThemeMode { DARK, LIGHT }

@Immutable
data class TuiColors(
    val mainBg: Color,
    val surfaceBg: Color,
    val headerBg: Color,
    val statusBarBg: Color,
    val statusBarFg: Color,
    val textFg: Color,
    val textDimFg: Color,
    val titleFg: Color,
    val borderFg: Color,
    val activeBorderFg: Color,
    val inactiveBorderFg: Color,
    val selectionBg: Color,
    val hoverBg: Color,
    val highlightFg: Color,
    val currentMarkerFg: Color,
    val progressFilledFg: Color,
    val progressEmptyFg: Color,
    val progressCornerFg: Color,
    val playBtnFg: Color,
    val modeFg: Color,
    val errorFg: Color,
    val dimBorderFg: Color,
) {
    companion object {
        fun dark() = TuiColors(
            mainBg = Color.Unspecified, // Color(red = 40, green = 42, blue = 54)
            surfaceBg = Color.Unspecified, // Color(red = 50, green = 52, blue = 66)
            headerBg = Color.Unspecified, // Color(red = 68, green = 71, blue = 90)
            statusBarBg = Color.Unspecified, //  Color(red = 33, green = 35, blue = 45)
            statusBarFg = Color(red = 120, green = 122, blue = 130),
            textFg = Color(red = 248, green = 248, blue = 242),
            textDimFg = Color(red = 104, green = 106, blue = 113),
            titleFg = Color(red = 139, green = 233, blue = 253),
            borderFg = Color(red = 68, green = 71, blue = 90),
            activeBorderFg = Color(red = 139, green = 233, blue = 253),
            inactiveBorderFg = Color(red = 58, green = 61, blue = 78),
            selectionBg = Color(red = 68, green = 71, blue = 90),
            hoverBg = Color(red = 52, green = 55, blue = 70),
            highlightFg = Color(red = 255, green = 184, blue = 108),
            currentMarkerFg = Color(red = 255, green = 85, blue = 85),
            progressFilledFg = Color(red = 80, green = 250, blue = 123),
            progressEmptyFg = Color(red = 68, green = 71, blue = 90),
            progressCornerFg = Color(red = 139, green = 233, blue = 253),
            modeFg = Color(red = 248, green = 248, blue = 242),
            playBtnFg = Color(red = 139, green = 233, blue = 253),
            errorFg = Color(red = 255, green = 85, blue = 85),
            dimBorderFg = Color(red = 68, green = 71, blue = 90),
        )

        fun light() = TuiColors(
            mainBg = Color.Unspecified, // Color(red = 250, green = 250, blue = 252)
            surfaceBg = Color.Unspecified, // Color(red = 240, green = 241, blue = 245)
            headerBg = Color.Unspecified, // Color(red = 228, green = 230, blue = 238)
            statusBarBg = Color.Unspecified, // Color(red = 200, green = 202, blue = 212)
            statusBarFg = Color(red = 98, green = 114, blue = 164),
            textFg = Color(red = 36, green = 41, blue = 46),
            textDimFg = Color(red = 140, green = 142, blue = 150),
            titleFg = Color(red = 9, green = 105, blue = 218),
            borderFg = Color(red = 208, green = 210, blue = 218),
            activeBorderFg = Color(red = 9, green = 105, blue = 218),
            inactiveBorderFg = Color(red = 225, green = 227, blue = 233),
            selectionBg = Color(red = 200, green = 216, blue = 245),
            hoverBg = Color(red = 232, green = 236, blue = 246),
            highlightFg = Color(red = 200, green = 100, blue = 10),
            currentMarkerFg = Color(red = 207, green = 34, blue = 46),
            progressFilledFg = Color(red = 30, green = 160, blue = 80),
            progressEmptyFg = Color(red = 208, green = 210, blue = 218),
            progressCornerFg = Color(red = 9, green = 105, blue = 218),
            modeFg = Color(red = 36, green = 41, blue = 46),
            playBtnFg = Color(red = 9, green = 105, blue = 218),
            errorFg = Color(red = 207, green = 34, blue = 46),
            dimBorderFg = Color(red = 208, green = 210, blue = 218),
        )
    }
}

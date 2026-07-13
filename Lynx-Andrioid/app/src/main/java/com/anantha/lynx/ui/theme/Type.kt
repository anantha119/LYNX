package com.anantha.lynx.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.anantha.lynx.R

/**
 * Named-instance font family, mirroring Lynx-IOS/Lynx-IOS/App/Theme.swift's
 * LynxFont enum. minSdk 23 can't interpolate variable-font weight axes, so
 * each weight ships as its own static .ttf (see app/src/main/res/font),
 * instanced from the same Syne-Variable.ttf / JetBrainsMono-Variable.ttf
 * bundled with the iOS app.
 */
object LynxFont {
    val displayFamily = FontFamily(
        Font(R.font.syne_regular, FontWeight.Normal),
        Font(R.font.syne_medium, FontWeight.Medium),
        Font(R.font.syne_semibold, FontWeight.SemiBold),
        Font(R.font.syne_bold, FontWeight.Bold),
        Font(R.font.syne_extrabold, FontWeight.ExtraBold),
    )

    val monoFamily = FontFamily(
        Font(R.font.jetbrains_mono_light, FontWeight.Light),
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    )

    fun display(size: TextUnit, weight: FontWeight = FontWeight.Bold): TextStyle = TextStyle(
        fontFamily = displayFamily,
        fontWeight = weight,
        fontSize = size,
    )

    fun mono(size: TextUnit, weight: FontWeight = FontWeight.Normal): TextStyle = TextStyle(
        fontFamily = monoFamily,
        fontWeight = weight,
        fontSize = size,
    )
}

val Typography = Typography(
    bodyLarge = LynxFont.mono(16.sp),
)

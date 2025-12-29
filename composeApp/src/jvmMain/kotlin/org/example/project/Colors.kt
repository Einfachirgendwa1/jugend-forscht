package org.example.project

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

val colorScheme = ColorScheme(
    primary = Mocha.Teal,
    onPrimary = Mocha.Base,
    primaryContainer = Mocha.Teal,
    onPrimaryContainer = Mocha.Base,
    inversePrimary = Mocha.Teal.inverse(),
    secondary = Mocha.Sapphire,
    onSecondary = Mocha.Mantle,
    secondaryContainer = Mocha.Sapphire,
    onSecondaryContainer = Mocha.Mantle,
    tertiary = Mocha.Sky,
    onTertiary = Mocha.Base,
    tertiaryContainer = Mocha.Sky,
    onTertiaryContainer = Mocha.Base,
    background = Mocha.Base,
    onBackground = Mocha.Text,
    surface = Mocha.Mantle,
    onSurface = Mocha.Mantle.inverse(),
    surfaceVariant = Mocha.Mantle,
    onSurfaceVariant = Mocha.Mantle.inverse(),
    surfaceTint = Mocha.Crust,
    inverseSurface = Mocha.Mantle.inverse(),
    inverseOnSurface = Mocha.Mantle,
    error = Mocha.Red,
    onError = Mocha.Red.inverse(),
    errorContainer = Mocha.Rosewater,
    onErrorContainer = Mocha.Rosewater.inverse(),
    outline = Mocha.Teal,
    outlineVariant = Mocha.Sapphire,
    scrim = Mocha.Crust,
    surfaceBright = Color.Unspecified,
    surfaceDim = Color.Unspecified,
    surfaceContainer = Color.Unspecified,
    surfaceContainerHigh = Color.Unspecified,
    surfaceContainerHighest = Color.Unspecified,
    surfaceContainerLow = Color.Unspecified,
    surfaceContainerLowest = Color.Unspecified,
    primaryFixed = Color.Unspecified,
    primaryFixedDim = Color.Unspecified,
    onPrimaryFixed = Color.Unspecified,
    onPrimaryFixedVariant = Color.Unspecified,
    secondaryFixed = Color.Unspecified,
    secondaryFixedDim = Color.Unspecified,
    onSecondaryFixed = Color.Unspecified,
    onSecondaryFixedVariant = Color.Unspecified,
    tertiaryFixed = Color.Unspecified,
    tertiaryFixedDim = Color.Unspecified,
    onTertiaryFixed = Color.Unspecified,
    onTertiaryFixedVariant = Color.Unspecified
)

@Suppress("unused")
object Mocha {
    val Rosewater: Color = Color(0xfff5e0dc)
    val Flamingo: Color = Color(0xfff2cdcd)
    val Pink: Color = Color(0xfff5c2e7)
    val Mauve: Color = Color(0xffcba6f7)
    val Red: Color = Color(0xfff38ba8)
    val Maroon: Color = Color(0xffeba0ac)
    val Peach: Color = Color(0xfffab387)
    val Yellow: Color = Color(0xfff9e2af)
    val Green: Color = Color(0xffa6e3a1)
    val Teal: Color = Color(0xff94e2d5)
    val Sky: Color = Color(0xff89dceb)
    val Sapphire: Color = Color(0xff74c7ec)
    val Blue: Color = Color(0xff89b4fa)
    val Lavender: Color = Color(0xffb4befe)
    val Text: Color = Color(0xffcdd6f4)
    val Subtext1: Color = Color(0xffbac2de)
    val Subtext0: Color = Color(0xffa6adc8)
    val Overlay2: Color = Color(0xff9399b2)
    val Overlay1: Color = Color(0xff7f849c)
    val Overlay0: Color = Color(0xff6c7086)
    val Surface2: Color = Color(0xff585b70)
    val Surface1: Color = Color(0xff45475a)
    val Surface0: Color = Color(0xff313244)
    val Base: Color = Color(0xff1e1e2e)
    val Mantle: Color = Color(0xff181825)
    val Crust: Color = Color(0xff11111b)
}

fun Color.inverse(): Color {
    return Color(
        red = 1f - red,
        green = 1f - green,
        blue = 1f - blue,
        alpha = alpha,
    )
}

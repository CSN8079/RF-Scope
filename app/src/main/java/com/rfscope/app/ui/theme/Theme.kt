package com.rfscope.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val ScopeBlack = Color(0xFF000000)
val ScopeGreen = Color(0xFF00FF00)
val ScopeGreenDim = Color(0xFF00B800)
val ScopeRed = Color(0xFFFF3B30)
val ScopeWhite = Color(0xFFF2F2F2)
val ScopeMuted = Color(0xFF888888)
val ScopePanel = Color(0xFF0A0A0A)
val ScopeLine = Color(0xFF202020)

private val ScopeColors = darkColorScheme(
    primary = ScopeGreen,
    onPrimary = ScopeBlack,
    secondary = ScopeGreenDim,
    background = ScopeBlack,
    onBackground = ScopeWhite,
    surface = ScopePanel,
    onSurface = ScopeWhite,
    error = ScopeRed,
)

private val ScopeTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 15.sp,
        color = ScopeWhite,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = ScopeWhite,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = ScopeMuted,
    ),
)

@Composable
fun RFScopeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScopeColors,
        typography = ScopeTypography,
        content = content,
    )
}

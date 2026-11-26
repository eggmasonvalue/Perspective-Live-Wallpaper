package com.perspectivelive.wallpaper.settings.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.perspectivelive.wallpaper.R

val GeistFontFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_regular, FontWeight.Medium),
    Font(R.font.geist_bold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
    Font(R.font.geist_bold, FontWeight.ExtraBold)
)

private val BaseTypography = Typography()

val PerspectiveTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = GeistFontFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = GeistFontFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = GeistFontFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = GeistFontFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = GeistFontFamily),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = GeistFontFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = GeistFontFamily),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = GeistFontFamily),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = GeistFontFamily),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = GeistFontFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = GeistFontFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = GeistFontFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = GeistFontFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = GeistFontFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = GeistFontFamily)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PerspectiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = PerspectiveTypography,
        content = content
    )
}

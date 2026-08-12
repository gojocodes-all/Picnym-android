package ng.name.gojodev.picnym.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PicnymPalette {
    val Ink = Color(0xFF111318)
    val Paper = Color(0xFFF4F0E7)
    val PaperBright = Color(0xFFFFFDF8)
    val Orange = Color(0xFFFF5C35)
    val Blue = Color(0xFF2457F5)
    val Green = Color(0xFF147A51)
    val Line = Color(0xFFC8C2B8)

    // Compatibility names used by existing screens.
    val Indigo = Blue
    val IndigoStrong = Color(0xFF173BB2)
    val Coral = Orange
    val Mint = Color(0xFF9CDFBD)
    val Lemon = Color(0xFFFFD66B)
}

enum class ThemeMode(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");
    companion object {
        fun from(value: String?): ThemeMode = entries.firstOrNull { it.key == value } ?: SYSTEM
    }
}

object AppThemeState {
    var mode: ThemeMode by mutableStateOf(ThemeMode.SYSTEM)
}

private val LightColors = lightColorScheme(
    primary = PicnymPalette.Orange,
    onPrimary = PicnymPalette.Ink,
    primaryContainer = Color(0xFFFFD8CD),
    onPrimaryContainer = Color(0xFF4A1002),
    secondary = PicnymPalette.Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE4FF),
    onSecondaryContainer = Color(0xFF07164D),
    tertiary = PicnymPalette.Green,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC5F2DA),
    onTertiaryContainer = Color(0xFF062F1D),
    background = PicnymPalette.Paper,
    onBackground = PicnymPalette.Ink,
    surface = PicnymPalette.PaperBright,
    onSurface = PicnymPalette.Ink,
    surfaceVariant = Color(0xFFEAE5DB),
    onSurfaceVariant = Color(0xFF5B5E63),
    outline = PicnymPalette.Line,
    outlineVariant = Color(0xFFE0DAD0),
    error = Color(0xFFA51F38),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF704C),
    onPrimary = Color(0xFF351004),
    primaryContainer = Color(0xFF6A210D),
    onPrimaryContainer = Color(0xFFFFD8CD),
    secondary = Color(0xFF88A2FF),
    onSecondary = Color(0xFF07164D),
    secondaryContainer = Color(0xFF1D357E),
    onSecondaryContainer = Color(0xFFDDE4FF),
    tertiary = Color(0xFF56C48E),
    onTertiary = Color(0xFF063923),
    tertiaryContainer = Color(0xFF174F36),
    onTertiaryContainer = Color(0xFFC5F2DA),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF3EFE6),
    surface = Color(0xFF191C22),
    onSurface = Color(0xFFF3EFE6),
    surfaceVariant = Color(0xFF25282E),
    onSurfaceVariant = Color(0xFFB8B6B1),
    outline = Color(0xFF4B4D52),
    outlineVariant = Color(0xFF33363C),
    error = Color(0xFFFF8FA3),
    onError = Color(0xFF4D0715)
)

private val PicnymTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 41.sp, letterSpacing = (-1.6).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 33.sp, lineHeight = 35.sp, letterSpacing = (-1.2).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = (-0.9).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 27.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.7.sp)
)

private val PicnymShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

@Composable
fun PicnymTheme(content: @Composable () -> Unit) {
    val dark = when (AppThemeState.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = PicnymTypography,
        shapes = PicnymShapes,
        content = content
    )
}

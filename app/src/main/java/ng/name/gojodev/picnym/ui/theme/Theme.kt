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
    val Indigo = Color(0xFF4C56E8)
    val IndigoStrong = Color(0xFF343CBD)
    val Coral = Color(0xFFFF6D59)
    val Mint = Color(0xFF53D6C5)
    val Lemon = Color(0xFFFFE58F)
    val Paper = Color(0xFFF7F7FB)
    val Ink = Color(0xFF17182B)
}

private val DarkBackground = Color(0xFF111221)
private val DarkSurface = Color(0xFF1A1B2D)

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
    primary = PicnymPalette.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E8FF),
    onPrimaryContainer = Color(0xFF22286F),
    secondary = PicnymPalette.Mint,
    onSecondary = Color(0xFF102D31),
    secondaryContainer = Color(0xFFDDF9F5),
    onSecondaryContainer = Color(0xFF174D48),
    tertiary = PicnymPalette.Coral,
    onTertiary = Color.White,
    background = PicnymPalette.Paper,
    onBackground = PicnymPalette.Ink,
    surface = Color.White,
    onSurface = PicnymPalette.Ink,
    surfaceVariant = Color(0xFFEEEFF7),
    onSurfaceVariant = Color(0xFF676A7D),
    outline = Color(0xFFD7D9E8),
    error = Color(0xFFB4233B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF939BFF),
    onPrimary = Color(0xFF151A61),
    primaryContainer = Color(0xFF2C3279),
    onPrimaryContainer = Color(0xFFE3E5FF),
    secondary = Color(0xFF6EE2D3),
    onSecondary = Color(0xFF073C36),
    secondaryContainer = Color(0xFF174D48),
    onSecondaryContainer = Color(0xFFDDF9F5),
    tertiary = Color(0xFFFF8C7A),
    onTertiary = Color(0xFF571609),
    background = DarkBackground,
    onBackground = Color(0xFFF1F1FA),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F1FA),
    surfaceVariant = Color(0xFF282A3E),
    onSurfaceVariant = Color(0xFFC1C3D1),
    outline = Color(0xFF42445A),
    error = Color(0xFFFFB3B8)
)

private val PicnymTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 38.sp, lineHeight = 41.sp, letterSpacing = (-1.3).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 27.sp, lineHeight = 31.sp, letterSpacing = (-0.7).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 18.sp)
)

private val PicnymShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
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

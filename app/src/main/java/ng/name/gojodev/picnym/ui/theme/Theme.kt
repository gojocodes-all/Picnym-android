package ng.name.gojodev.picnym.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF12264B)
private val Sage = Color(0xFFB9C9B2)
private val Cream = Color(0xFFF1F2ED)
private val Card = Color(0xFFFFFEFA)
private val Ink = Color(0xFF151B26)
private val DarkBg = Color(0xFF0D121A)
private val DarkCard = Color(0xFF171E29)
private val DarkInk = Color(0xFFF2F4F7)

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
    primary = Navy,
    onPrimary = Color.White,
    secondary = Sage,
    onSecondary = Navy,
    background = Cream,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9ECE6),
    onSurfaceVariant = Color(0xFF626B66),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC2D1FF),
    onPrimary = Color(0xFF071936),
    secondary = Color(0xFFC6D9C1),
    onSecondary = Color(0xFF1E3521),
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkCard,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF222B36),
    onSurfaceVariant = Color(0xFFBCC5CE),
    error = Color(0xFFFFB4AB)
)

@Composable
fun PicnymTheme(content: @Composable () -> Unit) {
    val dark = when (AppThemeState.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

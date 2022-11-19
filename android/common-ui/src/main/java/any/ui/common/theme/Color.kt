package any.ui.common.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import any.base.util.Sdk

val PrimaryColors = listOf(
    Color(0xFF785FD5),
    Color(0xFFFB8500),
    Color(0xFF049DBF),
    Color(0xFFAB17EB),
    Color(0xFFCF5432),
    Color(0xFF02CEAF),
    Color(0xFFF05D5E),
    Color(0xFF57D87C),
    Color(0xFF1B8EF2),
    Color(0xFFFDA7AE),
    Color(0xFF01B6AD),
    Color(0xFF8DB842)
)

val PrimaryColors_DarkMode = listOf(
    Color(0xFFB8A6FC),
    Color(0xFF6ADBD9),
    Color(0xFFFCBEBE),
    Color(0xFF55D07C),
    Color(0xFFF8ED89),
    Color(0xFF4EBBFF),
    Color(0xFFFF669E),
    Color(0xFFFDAD66),
    Color(0xFF35E2E6),
    Color(0xFFF183FF),
    Color(0xFF89D85D),
    Color(0xFFF37E7E)
)

val Primary = PrimaryColors[0]
val Primary_Night = PrimaryColors_DarkMode[0]

val Primary_Variant = PrimaryColors[0]
val Primary_Variant_Night = PrimaryColors_DarkMode[0]

val Secondary = Color(0xff90CBFB)
val Secondary_Night = Color(0xff90CBFB)

val Colors.primaryText: Color
    @Composable
    get() = onBackground

val Colors.warn: Color
    @Composable
    get() = if (isLight) Color(0xFFC59212) else Color(0xFFD39B0B)

val Colors.pass: Color
    @Composable
    get() = if (isLight) Color(0xFF4CAF50) else Color(0xFF64DD69)

val Colors.secondaryText: Color
    @Composable
    get() = onBackground.copy(alpha = 0.5f)

val Colors.thumbBorder: Color
    @Composable
    get() = if (isLight) Color(0xffbebebe) else Color(0xff333333)

val Colors.placeholder: Color
    @Composable
    get() = remember(onBackground, background) {
        onBackground.copy(alpha = 0.06f).compositeOver(background)
    }

val Colors.imagePlaceholder: Color
    @Composable
    get() = remember(onBackground, background) {
        onBackground.copy(alpha = 0.08f).compositeOver(background)
    }

val Colors.darkerImagePlaceholder: Color
    @Composable
    get() = remember(onBackground, background) {
        onBackground.copy(alpha = 0.12f).compositeOver(background)
    }

val Colors.primaryColors: List<Color>
    @Composable
    get() = if (isLight) PrimaryColors else PrimaryColors_DarkMode

val Colors.link: Color
    @Composable
    get() = if (isLight) primary else secondary

private const val BARS_ALPHA = 0.94f
private const val BRAS_ALPHA_LEGACY = 0.2f

val Colors.statusBar: Color
    get() = if (Sdk.hasAndroidM()) {
        Color.Transparent
    } else {
        Color.Black.copy(alpha = BRAS_ALPHA_LEGACY)
    }

val Colors.navigationBar: Color
    get() = if (Sdk.hasAndroidO()) {
        Color.Transparent
    } else {
        Color.Black.copy(alpha = BRAS_ALPHA_LEGACY)
    }

val Colors.topBarBackground: Color
    @Composable
    get() = background.copy(alpha = BARS_ALPHA)

val Colors.bottomBarBackground: Color
    @Composable
    get() = topBarBackground

val Colors.compositedStatusBarColor: Color
    @Composable
    get() {
        val foreground: Color = statusBar
        val background: Color = topBarBackground
        return remember(foreground, background) {
            foreground.compositeOver(background)
        }
    }

val Colors.compositedNavigationBarColor: Color
    @Composable
    get() {
        val foreground: Color = navigationBar
        val background: Color = bottomBarBackground
        return remember(foreground, background) {
            foreground.compositeOver(background)
        }
    }

val Colors.divider: Color
    @Composable
    get() = if (isLight) Color(0xffdedede) else Color(0xff555555)


fun Color.Companion.fromHex(hex: String): Color {
    return Color(android.graphics.Color.parseColor(hex))
}

fun Color.Companion.fromHexOrDefault(hex: String?, default: Color): Color {
    if (hex == null) {
        return default
    }
    return try {
        fromHex(hex)
    } catch (e: Exception) {
        default
    }
}

/**
 * Get theme color based on the current value of [Colors.isLight], if the
 * selected theme color is a [Color.Unspecified] or [Color.Transparent],
 * a [Colors.primary] will be returned.
 */
@Composable
fun themeColorOrPrimary(
    themeColor: Color,
    darkThemeColor: Color,
): Color {
    val color = if (MaterialTheme.colors.isLight ||
        (darkThemeColor == Color.Unspecified || darkThemeColor == Color.Transparent)
    ) {
        themeColor
    } else {
        darkThemeColor
    }
    return if (color != Color.Unspecified && color != Color.Transparent) {
        color
    } else {
        MaterialTheme.colors.primary
    }
}

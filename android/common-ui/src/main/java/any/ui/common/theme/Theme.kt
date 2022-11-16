package any.ui.common.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource

private val DarkColorPalette = darkColors(
    primary = Primary_Night,
    primaryVariant = Primary_Variant_Night,
    secondary = Secondary_Night,
    onSecondary = Color(0xffaaaaaa),
    surface = Color(0xff333333),
    error = Color(0xFFF34D4D)
)

private val LightColorPalette = lightColors(
    primary = Primary,
    primaryVariant = Primary_Variant,
    secondary = Secondary,
    onSecondary = Color(0xff777777),
    error = Color(0xFFFC4141)

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun AnyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColors: Boolean = false,
    primaryColor: Int = 0,
    darkModePrimaryColor: Int = 0,
    content: @Composable () -> Unit
) {
    val colors = remember(darkTheme, dynamicColors, primaryColor, darkModePrimaryColor) {
        if (darkTheme) {
            if (darkModePrimaryColor != 0) {
                DarkColorPalette.copy(primary = Color(darkModePrimaryColor))
            } else {
                DarkColorPalette
            }
        } else {
            if (primaryColor != 0) {
                LightColorPalette.copy(primary = Color(primaryColor))
            } else {
                LightColorPalette
            }
        }
    }
    val colors2 = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColors && darkTheme -> {
            colors.copy(background = dynamicDarkBackgroundColor())
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColors -> {
            colors.copy(background = dynamicLightBackgroundColor())
        }
        else -> colors
    }

    MaterialTheme(
        colors = colors2,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun dynamicLightBackgroundColor(): Color {
    return colorResource(android.R.color.system_neutral1_10)
}


@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun dynamicDarkBackgroundColor(): Color {
    return colorResource(android.R.color.system_neutral1_900)
}

package any.ui.post.sheet

import any.base.R as BaseR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.prefs.postFontScale
import any.base.prefs.postLineSpacingMultiplier
import any.base.prefs.preferencesStore
import any.base.util.compose.performTextHandleMove
import any.ui.common.theme.secondaryText
import any.ui.common.widget.BottomSheetTitle
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetState
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MIN_LINE_SPACING_MUL = 1f
private const val MAX_LINE_SPACING_MUL = 3f

private const val MIN_FONT_SCALE = 0.8f
private const val MAX_FONT_SCALE = 1.5f

@Composable
internal fun TextStyleSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    BottomSheet(
        state = state,
        modifier = modifier,
        skipPeeked = true,
    ) {
        TextStylePanel()
    }
}

@Composable
private fun TextStylePanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    val preferenceStore = LocalContext.current.preferencesStore()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        BottomSheetTitle(text = stringResource(BaseR.string.text_style))

        val lineSpacing by preferenceStore.postLineSpacingMultiplier
            .asStateFlow(scope)
            .collectAsState()

        ConfigItemHeader(
            text = stringResource(BaseR.string.line_spacing),
            value = lineSpacing.toString(),
        )

        SnappedSlider(
            value = lineSpacing,
            onValueChange = { preferenceStore.postLineSpacingMultiplier.value = it },
            minValue = MIN_LINE_SPACING_MUL,
            minLabel = MIN_LINE_SPACING_MUL.toString(),
            maxValue = MAX_LINE_SPACING_MUL,
            maxLabel = MAX_LINE_SPACING_MUL.toString(),
            steps = 19,
        )

        val fontScale by preferenceStore.postFontScale
            .asStateFlow(scope)
            .collectAsState()

        ConfigItemHeader(
            text = stringResource(BaseR.string.font_scale),
            value = fontScale.toString(),
        )

        SnappedSlider(
            value = fontScale,
            onValueChange = { preferenceStore.postFontScale.value = it },
            minValue = MIN_FONT_SCALE,
            minLabel = MIN_FONT_SCALE.toString(),
            maxValue = MAX_FONT_SCALE,
            maxLabel = MAX_FONT_SCALE.toString(),
            steps = 6,
        )
    }
}

@Composable
private fun SnappedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    minLabel: String,
    maxValue: Float,
    maxLabel: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueDecimals: Int = 2,
    labelsOffsetY: Dp = 8.dp,
) {
    val hapticFeedback = LocalHapticFeedback.current

    var previousValue by remember { mutableStateOf(0f) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = labelsOffsetY),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    color = MaterialTheme.colors.secondaryText,
                    fontSize = 14.sp,
                )
            ) {
                Text(text = minLabel)
                Text(text = maxLabel)
            }
        }
        Slider(
            value = value.coerceIn(minValue, maxValue),
            onValueChange = {
                val v = snapValue(it, minValue, maxValue, steps, valueDecimals)
                onValueChange(v)
                if (v != previousValue) {
                    hapticFeedback.performTextHandleMove()
                    previousValue = v
                }
            },
            valueRange = minValue..maxValue,
            steps = steps,
        )
    }
}

private fun snapValue(
    value: Float,
    minValue: Float,
    maxValue: Float,
    steps: Int,
    decimals: Int,
): Float {
    require(minValue < maxValue)
    require(steps > 0)

    if (value <= minValue) return minValue
    if (value >= maxValue) return maxValue

    val valuePerTick = ((maxValue - minValue) / (steps + 1)).round(decimals)

    val max = maxValue + valuePerTick
    var v = minValue
    while (v <= max) {
        if (abs(value - v) <= valuePerTick / 2) return v
        v = (v + valuePerTick).round(decimals)
    }

    error("Should not arrive here")
}

private fun Float.round(decimals: Int): Float {
    val f = 10f.pow(decimals)
    return (this * f.toInt()).roundToInt() / f
}
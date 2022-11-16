package any.ui.settings.ui

import any.base.R as BaseR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import any.base.prefs.darkModeEnabledFlow
import any.base.prefs.darkModePrimaryColor
import any.base.prefs.preferencesStore
import any.base.prefs.primaryColor
import any.ui.common.theme.PrimaryColors
import any.ui.common.theme.PrimaryColors_DarkMode
import any.ui.common.widget.BottomSheetTitle
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetState
import com.dokar.sheets.PeekHeight

@Composable
internal fun ColorThemePicker(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val preferencesStore = context.preferencesStore()

    val darkMode by preferencesStore.darkModeEnabledFlow(context, scope)
        .collectAsState()
    val primaryColor by preferencesStore.primaryColor
        .asStateFlow(scope)
        .collectAsState()
    val darkModePrimaryColor by preferencesStore.darkModePrimaryColor
        .asStateFlow(scope)
        .collectAsState()

    var lightModeColorSelectedIndex by remember {
        val index = PrimaryColors.indexOfFirst {
            it.toArgb() == primaryColor
        }
        mutableStateOf(if (index != -1) index else 0)
    }
    val onLightModeColorSelected: (Int, Color) -> Unit = remember {
        { index, color ->
            lightModeColorSelectedIndex = index
            preferencesStore.primaryColor.value = color.toArgb()
        }
    }

    var darkModeColorSelectedIndex by remember {
        val index = PrimaryColors_DarkMode.indexOfFirst {
            it.toArgb() == darkModePrimaryColor
        }
        mutableStateOf(if (index != -1) index else 0)
    }
    val onDarkModeColorSelected: (Int, Color) -> Unit = remember {
        { index, color ->
            darkModeColorSelectedIndex = index
            preferencesStore.darkModePrimaryColor.value = color.toArgb()
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        peekHeight = PeekHeight.fraction(1f),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BottomSheetTitle(stringResource(BaseR.string.colors))

            ColorSelector(
                title = stringResource(BaseR.string.light_theme),
                colors = PrimaryColors,
                selected = lightModeColorSelectedIndex,
                isActive = !darkMode,
                onSelected = onLightModeColorSelected
            )
            ColorSelector(
                title = stringResource(BaseR.string.dark_theme),
                colors = PrimaryColors_DarkMode,
                selected = darkModeColorSelectedIndex,
                isActive = darkMode,
                onSelected = onDarkModeColorSelected
            )
        }
    }
}

@Composable
private fun ColorSelector(
    title: String,
    modifier: Modifier = Modifier,
    colors: List<Color>,
    selected: Int,
    isActive: Boolean,
    onSelected: (index: Int, color: Color) -> Unit
) {
    val maxColumns = 6

    val groups = remember(colors) {
        colors.chunked(maxColumns)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp, 8.dp)
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(8.dp, 0.dp)
        ) {

            Text(title)

            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colors.primary,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(groups) { groupIndex, group ->
                ColorsRow(
                    colors = group,
                    groupIndex = groupIndex,
                    maxColumns = maxColumns,
                    selected = selected,
                    onSelected = onSelected
                )
            }
        }
    }
}

@Composable
private fun ColorsRow(
    colors: List<Color>,
    groupIndex: Int,
    maxColumns: Int,
    selected: Int,
    onSelected: (index: Int, color: Color) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        for (i in 0 until maxColumns) {
            val colorIndex = groupIndex * maxColumns + i
            if (i >= colors.size) {
                // Spacer item
                ColorItem(
                    color = Color.Transparent,
                    isSelected = false,
                    visible = false
                )
            } else {
                ColorItem(
                    color = colors[i],
                    isSelected = colorIndex == selected,
                    onClick = {
                        onSelected(colorIndex, colors[i])
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
    visible: Boolean = true
) {
    val alpha = remember(visible, isSelected) {
        if (visible) {
            1f
        } else {
            0f
        }
    }
    Spacer(
        modifier = Modifier
            .size(48.dp)
            .alpha(alpha)
            .clickable(
                enabled = visible,
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = rememberRipple(bounded = false),
                onClick = onClick ?: {}
            )
            .padding(8.dp)
            .let {
                if (isSelected) {
                    it.border(2.dp, color = color, shape = CircleShape)
                } else {
                    it
                }
            }
            .padding(4.dp)
            .background(color = color, shape = CircleShape)
    )
}

@Preview
@Composable
fun ItemPreview() {
    Row {
        ColorItem(MaterialTheme.colors.primary, false)
        ColorItem(MaterialTheme.colors.primary, true)
    }
}
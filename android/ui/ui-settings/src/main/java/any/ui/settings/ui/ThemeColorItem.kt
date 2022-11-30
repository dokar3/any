package any.ui.settings.ui

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.launch

@Composable
internal fun ThemeColorItem(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val colorPickerState = rememberBottomSheetState()

    SettingsItem(
        modifier = modifier,
        icon = {
            SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_color_lens_24))
        },
        onClick = {
            scope.launch {
                colorPickerState.expand()
            }
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(BaseR.string.theme_color))

            Spacer(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colors.primary,
                        shape = CircleShape
                    )
            )
        }
    }

    ColorThemePicker(colorPickerState)
}

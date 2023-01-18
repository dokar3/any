package any.ui.post.sheet

import any.base.R as BaseR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.model.DarkMode
import any.base.prefs.darkMode
import any.base.prefs.darkModeEnabledFlow
import any.base.prefs.darkenedImages
import any.base.prefs.monochromeImages
import any.base.prefs.preferencesStore
import any.base.prefs.transparentImages
import any.ui.common.widget.BottomSheetTitle
import any.ui.common.widget.FlatSwitch
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetState
import com.dokar.sheets.PeekHeight

@Composable
internal fun ThemeSettingsSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    BottomSheet(
        state = state,
        modifier = modifier,
        peekHeight = PeekHeight.fraction(1f),
    ) {
        ThemePanel()
    }
}

@Composable
private fun ThemePanel(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val preferencesStore = context.preferencesStore()

    val darkMode by preferencesStore.darkModeEnabledFlow(context, scope)
        .collectAsState()

    val darkenImages by preferencesStore.darkenedImages
        .asStateFlow(scope)
        .collectAsState()

    val monoImages by preferencesStore.monochromeImages
        .asStateFlow(scope)
        .collectAsState()

    val transparentImages by preferencesStore.transparentImages
        .asStateFlow(scope)
        .collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 8.dp,
                bottom = 16.dp,
            )
    ) {
        BottomSheetTitle(text = stringResource(BaseR.string.theme))

        SwitchItem(
            checked = darkMode,
            onCheckedChange = {
                preferencesStore.darkMode = if (it) DarkMode.Yes else DarkMode.No
            }
        ) {
            ConfigItemHeader(
                text = stringResource(BaseR.string.dark_mode),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = stringResource(BaseR.string.image_filters),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colors.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            SwitchItem(
                checked = darkenImages,
                onCheckedChange = { preferencesStore.darkenedImages.value = it },
                modifier = Modifier.weight(1f),
            ) {
                ConfigItemHeader(
                    text = stringResource(BaseR.string.darkened),
                    modifier = Modifier.weight(1f)
                )
            }

            SwitchItem(
                checked = monoImages,
                onCheckedChange = { preferencesStore.monochromeImages.value = it },
                modifier = Modifier.weight(1f),
            ) {
                ConfigItemHeader(
                    text = stringResource(BaseR.string.monochrome),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            SwitchItem(
                checked = transparentImages,
                onCheckedChange = { preferencesStore.transparentImages.value = it },
                modifier = Modifier.weight(1f),
            ) {
                ConfigItemHeader(
                    text = stringResource(BaseR.string.transparent),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SwitchItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        title()

        FlatSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

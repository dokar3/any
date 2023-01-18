package any.ui.settings.ui

import any.base.R as BaseR
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import any.ui.settings.SettingsViewModel

@Composable
internal fun UiSettings(
    viewModel: SettingsViewModel,
) {
    val res = LocalContext.current.resources
    LaunchedEffect(Unit) {
        viewModel.updateTitle(res.getString(BaseR.string.look_and_feel))
        viewModel.setShowBackArrow(true)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            DarkModeItem()
        }

        item {
            ImageFiltersItem()
        }

        item {
            ThemeColorItem()
        }

        item {
            HeaderImageItem()
        }

        item {
            ScrollBehaviorsItem()
        }
    }
}
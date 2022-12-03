package any.ui.settings.files

import any.base.R as BaseR
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import any.base.compose.StableHolder
import any.ui.settings.files.viewmodel.FilesAndDataViewModel
import any.ui.settings.viewmodel.SettingsViewModel

private const val ROUTE_FILES_SETTINGS = "settings/files_and_data/main"

@Composable
internal fun FilesAndDataSettings(
    settingsViewModel: SettingsViewModel,
    viewModel: FilesAndDataViewModel = viewModel(
        factory = FilesAndDataViewModel.Factory(LocalContext.current)
    ),
) {
    val subNavController = rememberNavController()
    NavHost(
        navController = subNavController,
        startDestination = ROUTE_FILES_SETTINGS,
    ) {
        composable(ROUTE_FILES_SETTINGS) {
            val res = LocalContext.current.resources
            LaunchedEffect(Unit) {
                settingsViewModel.updateTitle(res.getString(BaseR.string.files_and_data))
                settingsViewModel.setShowBackArrow(true)
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    val uiState by viewModel.filesUiState.collectAsState()
                    CleanableItems(cleanableItems = StableHolder(uiState.cleanableItems))
                }

                item {
                    CleanUpDbItem(onRequestCleanDb = { viewModel.cleanUpDatabase() })
                }

                item {
                    ImportExportItem(viewModel = viewModel)
                }
            }
        }
    }
}
package any.ui.settings.about

import any.base.R as BaseR
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import any.base.R
import any.base.util.Intents
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.ui.settings.SettingsItem
import any.ui.settings.viewmodel.SettingsViewModel

private object Links {
    const val Github = "https://github.com/dokar3/Any"
}

@Composable
internal fun AboutScreen(
    onNavigate: (NavEvent) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val res = LocalContext.current.resources
    LaunchedEffect(viewModel) {
        viewModel.updateTitle(res.getString(R.string.about))
        viewModel.setShowBackArrow(true)
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            SettingsItem(
                onClick = { onNavigate(navPushEvent(Routes.Settings.LIBS)) }
            ) {
                Text(stringResource(BaseR.string.libraries))
            }
        }

        item {
            val context = LocalContext.current
            SettingsItem(
                onClick = { Intents.openInBrowser(context, Links.Github) }
            ) {
                Text("Github")
            }
        }

        item {
            VersionsItem()
        }
    }
}
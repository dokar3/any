package any.ui.settings.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R
import any.base.util.Intents
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.secondaryText
import any.ui.settings.viewmodel.SettingsViewModel

@Immutable
private data class Lib(
    val name: String,
    val author: String,
    val link: String,
)

private val Libs = listOf(
    Lib(
        name = "accompanist",
        author = "Google",
        link = "https://github.com/google/accompanist",
    ),
    Lib(
        name = "androidx",
        author = "Google",
        link = "https://github.com/androidx/androidx"
    ),
    Lib(
        name = "coroutines",
        author = "JetBrains",
        link = "https://github.com/Kotlin/kotlinx.coroutines",
    ),
    Lib(
        name = "java-crc",
        author = "snksoft",
        link = "https://github.com/snksoft/java-crc",
    ),
    Lib(
        name = "DiskLruCache",
        author = "Jake Wharton",
        link = "https://github.com/JakeWharton/DiskLruCache",
    ),
    Lib(
        name = "duktape-android",
        author = "Square",
        link = "https://github.com/cashapp/zipline",
    ),
    Lib(
        name = "flick",
        author = "Saket Narayan",
        link = "https://github.com/saket/flick",
    ),
    Lib(
        name = "fresco",
        author = "Facebook",
        link = "https://github.com/facebook/fresco",
    ),
    Lib(
        name = "jsoup",
        author = "Jonathan Hedley",
        link = "https://github.com/jhy/jsoup",
    ),
    Lib(
        name = "kotlin",
        author = "JetBrains",
        link = "https://github.com/JetBrains/kotlin",
    ),
    Lib(
        name = "moshi",
        author = "Square",
        link = "https://github.com/square/moshi",
    ),
    Lib(
        name = "okhttp",
        author = "Square",
        link = "https://github.com/square/okhttp",
    ),
    Lib(
        name = "subsampling-scale-image-view",
        author = "davemorrissey",
        link = "https://github.com/davemorrissey/subsampling-scale-image-view",
    ),
    Lib(
        name = "turbine",
        author = "CashApp",
        link = "https://github.com/cashapp/turbine",
    ),
)

@Composable
internal fun LibsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.updateTitle(context.resources.getString(R.string.libraries))
        viewModel.setShowBackArrow(true)
    }

    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollBar(state),
    ) {
        items(Libs) {
            LibItem(
                onClick = { Intents.openInBrowser(context, it.link) },
                lib = it,
            )
        }
    }
}

@Composable
private fun LibItem(
    onClick: () -> Unit,
    lib: Lib,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(lib.name)

        Text(
            text = lib.author,
            color = MaterialTheme.colors.secondaryText,
            fontSize = 14.sp,
        )

        Text(
            text = lib.link,
            color = MaterialTheme.colors.secondaryText,
            fontSize = 14.sp,
        )
    }
}
package any.ui.settings.ui

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.base.prefs.darkenedImages
import any.base.prefs.monochromeImages
import any.base.prefs.preferencesStore
import any.base.prefs.transparentImages
import any.base.util.Intents
import any.ui.common.image.AsyncImage
import any.ui.common.richtext.Html
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.secondaryText
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon

@Composable
internal fun ImageFiltersItem(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val preferencesStore = context.preferencesStore()
    val darkenImages by preferencesStore.darkenedImages
        .asStateFlow(scope)
        .collectAsState()
    val monoImages by preferencesStore.monochromeImages
        .asStateFlow(scope)
        .collectAsState()
    val transparentImages by preferencesStore.transparentImages
        .asStateFlow(scope)
        .collectAsState()

    val onDarkenImagesChanged: (Boolean) -> Unit = { enabled ->
        preferencesStore.darkenedImages.value = enabled
    }
    val onMonoImagesChanged: (Boolean) -> Unit = { enabled ->
        preferencesStore.monochromeImages.value = enabled
    }
    val onTransparentImagesChanged: (Boolean) -> Unit = { enabled ->
        preferencesStore.transparentImages.value = enabled
    }

    SettingsItem(
        modifier = modifier,
        iconAlignment = Alignment.Top,
        icon = { SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_photo_24)) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(BaseR.string.image_filters))
            Spacer(modifier = Modifier.height(8.dp))

            AsyncImage(
                request = ImageRequest.Res.Builder(CommonUiR.drawable.sample_pic)
                    .diskCacheEnabled(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(5 / 2f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colors.imagePlaceholder)
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.secondaryText,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Html(
                    html = stringResource(BaseR.string.sample_image_info),
                    onLinkClick = { Intents.openInBrowser(context, it) },
                    fontSize = 14.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                CheckableButton(
                    title = stringResource(BaseR.string.darkened),
                    checked = darkenImages,
                    onCheckedChange = onDarkenImagesChanged,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(12.dp))

                CheckableButton(
                    title = stringResource(BaseR.string.monochrome),
                    checked = monoImages,
                    onCheckedChange = onMonoImagesChanged,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                CheckableButton(
                    title = stringResource(BaseR.string.transparent),
                    checked = transparentImages,
                    onCheckedChange = onTransparentImagesChanged,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

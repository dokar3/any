package any.ui.settings.ui

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.base.ImmutableHolder
import any.base.image.ImageLoader
import any.base.image.ImageRequest
import any.base.image.ImageState
import any.base.prefs.forceHeaderImageForAllServices
import any.base.prefs.headerImage
import any.base.prefs.preferencesStore
import any.base.util.queryName
import any.base.util.writeToFile
import any.ui.common.image.AsyncImage
import any.ui.common.theme.placeholder
import any.ui.common.theme.sizes
import any.ui.common.widget.BottomSheetTitle
import any.ui.common.widget.DefaultServiceHeader
import any.ui.common.widget.ProgressBar
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetValue
import com.dokar.sheets.PeekHeight
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun HeaderImageItem(
    modifier: Modifier = Modifier,
) {
    var showHeaderPicSelector by remember {
        mutableStateOf(false)
    }

    SettingsItem(
        modifier = modifier,
        icon = {
            SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_link_24))
        },
        onClick = {
            showHeaderPicSelector = true
        },
    ) {
        Text(text = stringResource(BaseR.string.header_image))
    }

    if (showHeaderPicSelector) {
        HeaderPicSelector(
            onDismissRequest = {
                showHeaderPicSelector = false
            },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HeaderPicSelector(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val preferencesStore = context.preferencesStore()
    val headerPicUrl by preferencesStore.headerImage
        .asStateFlow(scope)
        .collectAsState()
    val forceForAll by preferencesStore.forceHeaderImageForAllServices
        .asStateFlow(scope)
        .collectAsState()

    var text by remember(headerPicUrl) { mutableStateOf(headerPicUrl ?: "") }
    var headerPicUri: Uri? by remember { mutableStateOf(null) }
    var headerPicUriFilename: String? by remember { mutableStateOf(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        headerPicUri = uri
        if (uri != null) {
            scope.launch(Dispatchers.Default) {
                val filename = uri.queryName(context) ?: ""
                text = filename
                headerPicUriFilename = filename
            }
        }
    }

    val sheetState = rememberBottomSheetState()

    LaunchedEffect(sheetState) {
        sheetState.expand()
        snapshotFlow { sheetState.value }
            .distinctUntilChanged()
            .filter { it == BottomSheetValue.Collapsed }
            .collect {
                onDismissRequest()
            }
    }

    BottomSheet(
        state = sheetState,
        modifier = modifier,
        peekHeight = PeekHeight.fraction(1f),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            )
        ) {
            BottomSheetTitle(stringResource(BaseR.string.header_image))

            var isLoading by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .fillMaxWidth()
                    .aspectRatio(MaterialTheme.sizes.headerPicAspectRatio),
            ) {
                if (text.isNotEmpty()) {
                    AsyncImage(
                        request = if (headerPicUri != null) {
                            ImageRequest.Uri.Builder(headerPicUri!!)
                                .diskCacheEnabled(false)
                                .build()
                        } else {
                            ImageRequest.Downloadable(text)
                        },
                        contentDescription = stringResource(BaseR.string.header_image),
                        modifier = Modifier.background(MaterialTheme.colors.placeholder),
                        contentScale = ContentScale.Crop,
                        onState = { isLoading = it is ImageState.Loading },
                    )
                } else {
                    DefaultServiceHeader(
                        currentServiceName = "Any",
                        icons = ImmutableHolder(
                            List(20) { ImageRequest.Res(CommonUiR.drawable.sample_pic) }
                        ),
                    )
                }

                if (isLoading) {
                    ProgressBar(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 16.dp, y = (-16).dp),
                        size = 24.dp,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = {
                    headerPicUri = null
                    text = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onKeyEvent {
                        if (it.key.keyCode == Key.Backspace.keyCode && headerPicUri != null) {
                            text = ""
                            true
                        } else {
                            false
                        }
                    },
                placeholder = {
                    Text(stringResource(BaseR.string.enter_image_url))
                },
                label = {
                    Text(stringResource(BaseR.string.image_url))
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(CommonUiR.drawable.ic_baseline_photo_24),
                        contentDescription = stringResource(BaseR.string.select_image),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember {
                                    MutableInteractionSource()
                                },
                                indication = rememberRipple(bounded = false),
                                onClick = {
                                    importLauncher.launch("image/*")
                                }
                            )
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            val checkInteractionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .offset(x = (-12).dp)
                    .clickable(
                        interactionSource = checkInteractionSource,
                        indication = null,
                        onClick = {
                            preferencesStore.forceHeaderImageForAllServices.value = !forceForAll
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = forceForAll,
                    onCheckedChange = {
                        preferencesStore.forceHeaderImageForAllServices.value = it
                    },
                    interactionSource = checkInteractionSource,
                )
                Text(stringResource(BaseR.string.force_for_all_services))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val uri = headerPicUri
                            if (uri != null && text == headerPicUriFilename) {
                                val headerPicFile = File(context.filesDir, "header_pic")
                                uri.writeToFile(context, headerPicFile)

                                val previousUrl = preferencesStore.headerImage.value
                                if (previousUrl != null) {
                                    ImageLoader.evictFromCache(ImageRequest.Url(previousUrl))
                                }

                                preferencesStore.headerImage.value = headerPicFile.absolutePath
                            } else {
                                if (text.isNotEmpty()) {
                                    preferencesStore.headerImage.value = text
                                } else {
                                    preferencesStore.headerImage.value = null
                                }
                            }
                        }
                        sheetState.collapse()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}
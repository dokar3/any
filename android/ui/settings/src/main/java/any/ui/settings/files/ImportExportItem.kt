package any.ui.settings.files

import android.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import any.base.compose.StableHolder
import any.base.util.Dirs
import any.base.util.FileUtil
import any.ui.common.dialog.BasicDialog
import any.ui.common.theme.pass
import any.ui.common.theme.secondaryText
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

private const val BACKUP_FILE_MIME_TYPE = "application/zip"

@Composable
internal fun ImportExportItem(
    viewModel: FilesAndDataViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var outputUri: Uri? by remember {
        mutableStateOf(null)
    }

    var inputUri: Uri? by remember {
        mutableStateOf(null)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        CreateDocument(BACKUP_FILE_MIME_TYPE)
    ) { uri ->
        outputUri = uri
    }

    val importLauncher = rememberLauncherForActivityResult(GetContent()) { uri ->
        inputUri = uri
    }

    var showExportDialog by remember { mutableStateOf(false) }

    var showImportDialog by remember { mutableStateOf(false) }

    fun newTempBackupFile(): File {
        val dir = Dirs.backupTempDir(context)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Cannot create temp dir for backup: $dir")
        }
        val file = File(dir, "backup.zip")
        if (file.exists()) {
            file.delete()
        }
        return file
    }

    // export
    LaunchedEffect(outputUri) {
        val output = outputUri ?: return@LaunchedEffect
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val tempFile = newTempBackupFile()
            viewModel.exportSelectedBackupItems(backupFile = tempFile)
            delay(10)
            viewModel.backupUiState
                .filterNot { it.isExporting }
                .first()
            FileUtil.copyToUri(
                context = context,
                input = tempFile,
                output = output,
            )
            tempFile.delete()
            outputUri = null
        }
    }

    // import
    LaunchedEffect(inputUri) {
        val input = inputUri ?: return@LaunchedEffect
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val tempFile = newTempBackupFile()
            FileUtil.copyToFile(
                context = context,
                input = input,
                output = tempFile,
            )
            viewModel.loadBackupItemsToImport(tempFile)
            showImportDialog = true
            inputUri = null
        }
    }

    Column(modifier = modifier) {
        SettingsItem(
            iconAlignment = Alignment.Top,
            icon = { SettingsItemIcon(painterResource(CommonUiR.drawable.ic_export)) },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(BaseR.string.export_description))

                OutlinedButton(
                    onClick = {
                        showExportDialog = true
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(BaseR.string.export))
                }
            }
        }

        SettingsItem(
            iconAlignment = Alignment.Top,
            icon = { SettingsItemIcon(painterResource(CommonUiR.drawable.ic_import)) },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(BaseR.string.import_description))

                OutlinedButton(
                    onClick = {
                        importLauncher.launch(BACKUP_FILE_MIME_TYPE)
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(BaseR.string.import_))
                }
            }
        }
    }

    if (showExportDialog) {
        val uiState by viewModel.backupUiState.collectAsState()

        LaunchedEffect(viewModel) {
            viewModel.loadBackupItemsToExport()
        }

        BackupItemSelectionDialog(
            uiState = uiState,
            onDismissRequest = { showExportDialog = false },
            onConfirm = {
                if (uiState.isExported) {
                    showExportDialog = false
                } else {
                    exportLauncher.launch("any_backup_${getNowDateTime()}.zip")
                }
            },
            title = { Text(stringResource(BaseR.string.export)) },
            confirmText = {
                Text(
                    text = if (uiState.isExported) {
                        stringResource(BaseR.string.done)
                    } else {
                        stringResource(BaseR.string.export)
                    }
                )
            },
            showSelection = !uiState.isExporting && !uiState.isExported,
            isBackupRunning = uiState.isExporting,
        )
    }

    if (showImportDialog) {
        val uiState by viewModel.backupUiState.collectAsState()

        val onDismissRequest: () -> Unit = remember {
            {
                showImportDialog = false
                // Clear temp files
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    Dirs.backupTempDir(context).deleteRecursively()
                }
            }
        }

        BackupItemSelectionDialog(
            uiState = uiState,
            onDismissRequest = onDismissRequest,
            onConfirm = {
                if (uiState.isImported) {
                    onDismissRequest()
                } else {
                    viewModel.importSelectedBackupItems()
                }
            },
            title = {
                val title = if (uiState.isLoadingBackup) {
                    stringResource(BaseR.string.loading)
                } else {
                    stringResource(BaseR.string.import_)
                }
                Text(title)
            },
            confirmText = {
                Text(
                    text = if (uiState.isImported) {
                        stringResource(BaseR.string.done)
                    } else {
                        stringResource(BaseR.string.import_)
                    }
                )
            },
            showSelection = !uiState.isImporting && !uiState.isImported,
            isBackupRunning = uiState.isImporting || uiState.isLoadingBackup,
        )
    }
}

@Composable
private fun BackupItemSelectionDialog(
    uiState: BackupUiState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: @Composable () -> Unit,
    confirmText: @Composable () -> Unit,
    showSelection: Boolean,
    modifier: Modifier = Modifier,
    isBackupRunning: Boolean = false,
) {
    val selectedCount = remember(uiState.items) {
        uiState.items.count { it.isSelected }
    }
    BasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title,
        cancelText = { Text(stringResource(R.string.cancel)) },
        confirmText = confirmText,
        cancelEnabled = !isBackupRunning,
        confirmEnabled = selectedCount != 0 && !isBackupRunning,
        onConfirmClick = { onConfirm() },
        dismissOnConfirm = false,
        dismissOnClickOutside = !isBackupRunning,
        dismissOnBackPress = !isBackupRunning,
    ) {
        Box {
            BackupItemList(
                types = StableHolder(uiState.items),
                showSelection = showSelection,
            )

            if (isBackupRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BackupItemList(
    types: StableHolder<List<BackupUiItem>>,
    showSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = types.value) {
            BackupItem(item = it, showSelection = showSelection)
        }
    }
}

@Composable
private fun BackupItem(
    item: BackupUiItem,
    showSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = {
                    if (item.isSelected) {
                        item.unselect()
                    } else {
                        item.select()
                    }
                },
                enabled = showSelection,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val passColor = MaterialTheme.colors.pass
        val secondaryTextColor = MaterialTheme.colors.secondaryText
        val text = remember(item, secondaryTextColor, passColor) {
            buildAnnotatedString {
                append(item.typeName)
                append(' ')
                withStyle(SpanStyle(color = secondaryTextColor)) {
                    append('(')
                    if (item.successCount > 0) {
                        withStyle(SpanStyle(color = passColor)) {
                            append("${item.successCount}")
                        }
                        append(" / ")
                    }
                    append("${item.count})")
                }
            }
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f)
        )

        if (showSelection) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = {
                    if (item.isSelected) {
                        item.unselect()
                    } else {
                        item.select()
                    }
                },
                interactionSource = interactionSource,
            )
        }
    }
}

private fun getNowDateTime(): String {
    val format = SimpleDateFormat.getInstance()
    val date = format.format(System.currentTimeMillis()).replace("/", "_")
    return FileUtil.buildValidFatFilename(date)
}

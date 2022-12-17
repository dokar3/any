package any.ui.settings.dev

import any.ui.common.R as CommonUiR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.util.CrashHandler
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.widget.BottomSheetTitle
import any.ui.settings.SettingsViewModel
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.PeekHeight
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CrashLogsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    var crashLogFiles: List<File> by remember {
        mutableStateOf(emptyList())
    }

    var selectedLogFile: File? by remember {
        mutableStateOf(null)
    }

    val logSheetState = rememberBottomSheetState()

    LaunchedEffect(viewModel) {
        viewModel.updateTitle("Crash logs")
        viewModel.setShowBackArrow(true)

        crashLogFiles = CrashHandler.crashLogFiles()
            .sortedByDescending { it.name }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.verticalScrollBar(listState),
            state = listState,
        ) {
            if (crashLogFiles.isEmpty()) {
                item {
                    Text(
                        text = "Nothing here",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            items(crashLogFiles) {
                Text(
                    text = it.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedLogFile = it
                            scope.launch {
                                logSheetState.expand()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    crashLogFiles.forEach(File::delete)
                    crashLogFiles = emptyList()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset((-32).dp, (-32).dp),
        ) {
            Icon(
                painter = painterResource(CommonUiR.drawable.ic_baseline_clear_all_24),
                contentDescription = "Clear all",
                tint = Color.White,
            )
        }
    }

    BottomSheet(
        state = logSheetState,
        peekHeight = PeekHeight.fraction(1f),
    ) {
        var logs by remember {
            mutableStateOf("")
        }

        LaunchedEffect(selectedLogFile) {
            logs = selectedLogFile?.readText() ?: ""
        }

        Column(modifier = Modifier.padding(8.dp)) {
            BottomSheetTitle(selectedLogFile?.name ?: "No selected log file")

            Spacer(modifier = Modifier.height(8.dp))

            SelectionContainer(
                modifier = Modifier.verticalScroll(state = rememberScrollState())
            ) {
                Text(
                    text = logs,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}
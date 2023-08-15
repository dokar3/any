package any.ui.common.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberTextSnackBarState(): TextSnackBarState {
    return remember {
        TextSnackBarState()
    }
}

class TextSnackBarState {

    val text = mutableStateOf<String?>(null)

    val visible = mutableStateOf(false)

    private var dismissJob: Job? = null

    suspend fun show(
        text: String,
        duration: Long = DEFAULT_DURATION
    ) = coroutineScope {
        this@TextSnackBarState.text.value = text
        visible.value = true

        dismissJob?.cancel()
        dismissJob = launch {
            delay(duration)
            dismiss()
        }
    }

    suspend fun dismiss() {
        text.value = null
        visible.value = false
        dismissJob?.cancel()
    }

    companion object {

        const val DEFAULT_DURATION = 2500L
    }
}

@Composable
fun TextSnackBar(
    state: TextSnackBarState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    backgroundColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.96f)
) {
    if (!state.visible.value) {
        return
    }

    Snackbar(
        modifier = modifier
            .padding(16.dp, 0.dp),
        shape = shape,
        backgroundColor = backgroundColor,
    ) {
        Text(state.text.value ?: "")
    }
}
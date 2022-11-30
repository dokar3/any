package any.ui.jslogger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import any.base.R

@Composable
internal fun floatingContentArrowColor(): Color {
    val surface = MaterialTheme.colors.surface
    val onSurface = MaterialTheme.colors.onSurface
    return remember(surface, onSurface) {
        onSurface.copy(alpha = 0.06f).compositeOver(surface)
    }
}

@Composable
internal fun FloatingLoggerScreen(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.js_logs),
            modifier = Modifier
                .fillMaxWidth()
                .background(floatingContentArrowColor())
                .padding(vertical = 6.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        LoggerScreen()
    }
}

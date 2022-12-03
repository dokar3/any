package any.base.util.compose

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

fun HapticFeedback.performLongPress() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

fun HapticFeedback.performTextHandleMove() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}
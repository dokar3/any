package any.ui.common

import android.view.ViewConfiguration as AndroidViewConfiguration
import android.content.Context
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal open class ViewConfigurationImpl(
    context: Context
) : ViewConfiguration {
    private val viewConfiguration: android.view.ViewConfiguration =
        AndroidViewConfiguration.get(context)

    override val longPressTimeoutMillis: Long
        get() = AndroidViewConfiguration.getLongPressTimeout().toLong()

    override val doubleTapTimeoutMillis: Long
        get() = AndroidViewConfiguration.getDoubleTapTimeout().toLong()

    override val doubleTapMinTimeMillis: Long
        get() = 40

    override val touchSlop: Float
        get() = viewConfiguration.scaledTouchSlop.toFloat()

    override val minimumTouchTargetSize: DpSize
        get() = super.minimumTouchTargetSize
}

/**
 * Customizable AndroidViewConfiguration, copied from 'androidx.compose.ui'
 *
 * @see [AndroidViewConfiguration]
 * @see [ViewConfiguration]
 */
fun ViewConfiguration(
    context: Context,
    longPressTimeoutMillis: Long = AndroidViewConfiguration.getLongPressTimeout().toLong(),
    doubleTapTimeoutMillis: Long = AndroidViewConfiguration.getDoubleTapTimeout().toLong(),
    doubleTapMinTimeMillis: Long = 40,
    minimumTouchTargetSize: DpSize = DpSize(42.dp, 42.dp),
): ViewConfiguration {
    return object : ViewConfigurationImpl(context) {
        override val longPressTimeoutMillis: Long = longPressTimeoutMillis
        override val doubleTapTimeoutMillis: Long = doubleTapTimeoutMillis
        override val doubleTapMinTimeMillis: Long = doubleTapMinTimeMillis
        override val minimumTouchTargetSize: DpSize = minimumTouchTargetSize
    }
}
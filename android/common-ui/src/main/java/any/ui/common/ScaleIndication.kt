package any.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale

@Composable
fun rememberScale(targetScale: Float = 0.95f): Indication {
    return remember(targetScale) { ScaleIndication(targetScale) }
}

class ScaleIndication(private val targetScale: Float) : Indication {
    @Composable
    override fun rememberUpdatedInstance(
        interactionSource: InteractionSource
    ): IndicationInstance {
        val isPressed by interactionSource.collectIsPressedAsState()
        val instance = remember { ScaleIndicationInstance() }
        LaunchedEffect(isPressed) {
            if (isPressed) {
                instance.scale.animateTo(targetScale)
            } else {
                instance.scale.animateTo(1f)
            }
        }
        return instance
    }
}

class ScaleIndicationInstance : IndicationInstance {
    val scale = Animatable(1f)

    override fun ContentDrawScope.drawIndication() {
        val scope = this
        scale(scale.value) {
            scope.drawContent()
        }
    }
}

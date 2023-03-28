// Copied and modified from androidx.compose.material.AndroidAlertDialog.android.kt

/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package any.ui.common.widget

import android.content.Context
import android.graphics.Outline
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.annotation.StyleRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID


/**
 * Opens a dialog with the given content.
 *
 * A dialog is a small window that prompts the user to make a decision or enter
 * additional information. A dialog does not fill the screen and is normally used
 * for modal events that require users to take an action before they can proceed.
 *
 * The dialog is visible as long as it is part of the composition hierarchy.
 * In order to let the user dismiss the Dialog, the implementation of [onDismissRequest] should
 * contain a way to remove to remove the dialog from the composition hierarchy.
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.DialogSample
 *
 * @param onDismissRequest Executes when the user tries to dismiss the dialog.
 * @param properties [DialogProperties] for further customization of this dialog's behavior.
 * @param content The content to be displayed inside the dialog.
 */
@Composable
fun StyleableDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    @StyleRes themeResId: Int = 0,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val dialogId = rememberSaveable { UUID.randomUUID() }
    val dialog = remember(view, density, themeResId) {
        DialogWrapper(
            onDismissRequest,
            properties,
            view,
            layoutDirection,
            density,
            dialogId,
            themeResId
        ).apply {
            setContent(composition) {
                // TODO(b/159900354): draw a scrim and add margins around the Compose Dialog, and
                //  consume clicks so they can't pass through to the underlying UI
                DialogLayout(
                    Modifier.semantics { dialog() },
                ) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(dialog) {
        dialog.show()

        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    SideEffect {
        dialog.updateParameters(
            onDismissRequest = onDismissRequest,
            properties = properties,
            layoutDirection = layoutDirection
        )
    }
}

@Suppress("ViewConstructor")
private class DialogLayout(
    context: Context,
    override val window: Window
) : AbstractComposeView(context), DialogWindowProvider {
    private var content: @Composable () -> Unit by mutableStateOf({})

    var usePlatformDefaultWidth = false

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }
}

private class DialogWrapper(
    private var onDismissRequest: () -> Unit,
    private var properties: DialogProperties,
    private val composeView: View,
    layoutDirection: LayoutDirection,
    density: Density,
    dialogId: UUID,
    @StyleRes themeResId: Int,
) : ComponentDialog(
    /**
     * [Window.setClipToOutline] is only available from 22+, but the style attribute exists on 21.
     * So use a wrapped context that sets this attribute for compatibility back to 21.
     */
    ContextThemeWrapper(composeView.context, R.style.DialogWindowTheme),
    themeResId
),
    ViewRootForInspector {

    private val dialogLayout: DialogLayout

    private val maxSupportedElevation = 30.dp

    override val subCompositionView: AbstractComposeView get() = dialogLayout

    private val defaultSoftInputMode: Int

    init {
        val window = window ?: error("Dialog has no window")
        defaultSoftInputMode =
            window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        dialogLayout = DialogLayout(context, window).apply {
            // Set unique id for AbstractComposeView. This allows state restoration for the state
            // defined inside the Dialog via rememberSaveable()
            setTag(R.id.compose_view_saveable_id_tag, "Dialog:$dialogId")
            // Enable children to draw their shadow by not clipping them
            clipChildren = false
            // Allocate space for elevation
            with(density) { elevation = maxSupportedElevation.toPx() }
            // Simple outline to force window manager to allocate space for shadow.
            // Note that the outline affects clickable area for the dismiss listener. In case of
            // shapes like circle the area for dismiss might be to small (rectangular outline
            // consuming clicks outside of the circle).
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, result: Outline) {
                    result.setRect(0, 0, view.width, view.height)
                    // We set alpha to 0 to hide the view's shadow and let the composable to draw
                    // its own shadow. This still enables us to get the extra space needed in the
                    // surface.
                    result.alpha = 0f
                }
            }
        }

        /**
         * Disables clipping for [this] and all its descendant [ViewGroup]s until we reach a
         * [DialogLayout] (the [ViewGroup] containing the Compose hierarchy).
         */
        fun ViewGroup.disableClipping() {
            clipChildren = false
            if (this is DialogLayout) return
            for (i in 0 until childCount) {
                (getChildAt(i) as? ViewGroup)?.disableClipping()
            }
        }

        // Turn of all clipping so shadows can be drawn outside the window
        (window.decorView as? ViewGroup)?.disableClipping()
        setContentView(dialogLayout)
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        // Initial setup
        updateParameters(onDismissRequest, properties, layoutDirection)

        // Due to how the onDismissRequest callback works
        // (it enforces a just-in-time decision on whether to update the state to hide the dialog)
        // we need to unconditionally add a callback here that is always enabled,
        // meaning we'll never get a system UI controlled predictive back animation
        // for these dialogs
        onBackPressedDispatcher.addCallback(this) {
            if (properties.dismissOnBackPress) {
                onDismissRequest()
            }
        }
    }

    private fun setLayoutDirection(layoutDirection: LayoutDirection) {
        dialogLayout.layoutDirection = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
    }

    // TODO(b/159900354): Make the Android Dialog full screen and the scrim fully transparent

    fun setContent(parentComposition: CompositionContext, children: @Composable () -> Unit) {
        dialogLayout.setContent(parentComposition, children)
    }

    private fun setSecurePolicy(securePolicy: SecureFlagPolicy) {
        val secureFlagEnabled =
            securePolicy.shouldApplySecureFlag(composeView.isFlagSecureEnabled())
        window!!.setFlags(
            if (secureFlagEnabled) {
                WindowManager.LayoutParams.FLAG_SECURE
            } else {
                WindowManager.LayoutParams.FLAG_SECURE.inv()
            },
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun updateParameters(
        onDismissRequest: () -> Unit,
        properties: DialogProperties,
        layoutDirection: LayoutDirection
    ) {
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        setSecurePolicy(properties.securePolicy)
        setLayoutDirection(layoutDirection)
        dialogLayout.usePlatformDefaultWidth = properties.usePlatformDefaultWidth
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (properties.decorFitsSystemWindows) {
                window?.setSoftInputMode(defaultSoftInputMode)
            } else {
                @Suppress("DEPRECATION")
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
    }

    fun disposeComposition() {
        dialogLayout.disposeComposition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        if (result && properties.dismissOnClickOutside) {
            onDismissRequest()
        }

        return result
    }

    override fun cancel() {
        // Prevents the dialog from dismissing itself
        return
    }
}

@Composable
private fun DialogLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.fastMap { it.measure(constraints) }
        val width = placeables.fastMaxBy { it.width }?.width ?: constraints.minWidth
        val height = placeables.fastMaxBy { it.height }?.height ?: constraints.minHeight
        layout(width, height) {
            placeables.fastForEach { it.placeRelative(0, 0) }
        }
    }
}

internal fun SecureFlagPolicy.shouldApplySecureFlag(isSecureFlagSetOnParent: Boolean): Boolean {
    return when (this) {
        SecureFlagPolicy.SecureOff -> false
        SecureFlagPolicy.SecureOn -> true
        SecureFlagPolicy.Inherit -> isSecureFlagSetOnParent
    }
}

internal fun View.isFlagSecureEnabled(): Boolean {
    val windowParams = rootView.layoutParams as? WindowManager.LayoutParams
    if (windowParams != null) {
        return (windowParams.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
    return false
}

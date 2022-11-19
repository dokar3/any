package any.ui.common.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import any.base.ImmutableHolder
import any.base.util.compose.performLongPress
import any.domain.entity.UiServiceManifest

@Composable
fun ServiceDropdownButton(
    onSelectService: (UiServiceManifest) -> Unit,
    onServiceManagementClick: () -> Unit,
    onLongClickCurrentService: () -> Unit,
    services: ImmutableHolder<List<UiServiceManifest>>,
    currentService: UiServiceManifest?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = 2,
    dropdownAlignmentToAnchor: Alignment = Alignment.TopEnd,
    dropdownTransformOrigin: TransformOrigin = TransformOrigin(1f, 0f),
) {
    if (currentService == null) {
        return
    }

    var showServicesDropDown by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        val hapticFeedback = LocalHapticFeedback.current
        DropdownButton(
            isExpanded = showServicesDropDown,
            onClick = { showServicesDropDown = !showServicesDropDown },
            onLongClick = {
                onLongClickCurrentService()
                hapticFeedback.performLongPress()
            },
        ) {
            Text(
                text = currentService.name,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showServicesDropDown) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
            ) {
                ServicesDropdown(
                    services = services,
                    selected = currentService,
                    onDismissRequest = { showServicesDropDown = false },
                    onManagementClick = onServiceManagementClick,
                    onSelectedItem = onSelectService,
                    contentAlignmentToAnchor = dropdownAlignmentToAnchor,
                    transformOrigin = dropdownTransformOrigin,
                )
            }
        }
    }
}

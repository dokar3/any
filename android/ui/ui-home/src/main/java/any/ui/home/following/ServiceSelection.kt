package any.ui.home.following

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.compose.ImmutableHolder
import any.ui.common.theme.themeColorOrPrimary
import any.ui.common.widget.Avatar
import any.ui.home.HomeScreenDefaults
import any.ui.home.following.viewmodel.ServiceOfFollowingUsers

@Composable
internal fun ServiceSelection(
    onItemClick: (ServiceOfFollowingUsers) -> Unit,
    showAsRow: Boolean,
    services: ImmutableHolder<List<ServiceOfFollowingUsers>>,
    modifier: Modifier = Modifier,
) {
    if (showAsRow) {
        ServiceSelectionRow(
            onItemClick = onItemClick,
            services = services,
            modifier = modifier,
        )
    }
}

@Composable
private fun ServiceSelectionRow(
    onItemClick: (ServiceOfFollowingUsers) -> Unit,
    services: ImmutableHolder<List<ServiceOfFollowingUsers>>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = HomeScreenDefaults.ListPadding,
    ) {
        items(services.value) {
            SelectableServiceItem(
                onClick = { onItemClick(it) },
                service = it,
            )
        }
    }
}

@Composable
private fun SelectableServiceItem(
    onClick: () -> Unit,
    service: ServiceOfFollowingUsers,
    modifier: Modifier = Modifier,
    serviceIconSize: Dp = 20.dp,
    selectedBackgroundAlpha: Float = if (MaterialTheme.colors.isLight) 0.24f else 0.5f,
) {
    val themeColor = themeColorOrPrimary(
        themeColor = Color(service.themeColor),
        darkThemeColor = Color(service.darkThemeColor),
    )
    val backgroundColor = if (service.isSelected) {
        themeColor.copy(alpha = selectedBackgroundAlpha)
    } else {
        Color.Transparent
    }
    val contentColor = contentColorFor(backgroundColor)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(color = backgroundColor, shape = CircleShape)
            .border(width = 1.dp, color = themeColor, shape = CircleShape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (service.isAll) {
            Icon(
                painter = painterResource(CommonUiR.drawable.ic_app),
                contentDescription = null,
                modifier = Modifier.size(serviceIconSize),
                tint = contentColor,
            )
        } else {
            Avatar(
                name = service.name,
                url = service.icon,
                size = serviceIconSize,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val name = if (service.isAll) {
            stringResource(BaseR.string.all)
        } else {
            service.name
        }
        Text(
            text = "$name (${service.userCount})",
            fontSize = 14.sp,
            maxLines = 1,
            color = contentColor,
        )
    }
}
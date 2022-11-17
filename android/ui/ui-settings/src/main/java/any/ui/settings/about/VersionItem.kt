package any.ui.settings.about

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.util.AppUtil
import any.data.js.ServiceApiVersion
import any.ui.settings.BuildConfig
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun VersionsItem(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val builtAt = remember {
        val format = SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault())
        format.format(BuildConfig.BUILT_AT)
    }

    val headerFontSize = 13.sp

    SettingsItem(
        modifier = modifier,
        iconAlignment = Alignment.Top,
        icon = { SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_info_24)) },
    ) {
        Column {
            Text(
                text = stringResource(BaseR.string.version),
                fontSize = headerFontSize,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )

            Text(
                AppUtil.getAppVersionName(context) + if (BuildConfig.DEBUG) " (debug)" else "",
                fontSize = headerFontSize
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(BaseR.string.service_api_version),
                fontSize = headerFontSize,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )

            Text(ServiceApiVersion.get(), fontSize = headerFontSize)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(BaseR.string.build_time),
                fontSize = headerFontSize,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )

            Text(builtAt, fontSize = headerFontSize)
        }
    }
}

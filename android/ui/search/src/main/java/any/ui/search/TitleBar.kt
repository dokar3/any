package any.ui.search

import any.base.R as BaseR
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import any.base.compose.ImmutableHolder
import any.domain.entity.UiServiceManifest
import any.ui.common.theme.sizes
import any.ui.common.widget.SearchBar
import any.ui.common.widget.SearchBarState
import any.ui.common.widget.ServiceDropdownButton

@Composable
internal fun TitleBar(
    uiState: SearchUiState,
    onBack: () -> Unit,
    onUpdateQuery: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onManagementClick: () -> Unit,
    onSelectService: (UiServiceManifest) -> Unit,
    modifier: Modifier = Modifier,
    searchBarState: SearchBarState,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.sizes.titleBarHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(BaseR.string.back),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        SearchBar(
            text = uiState.query,
            state = searchBarState,
            onValueChange = onUpdateQuery,
            modifier = Modifier
                .weight(1f)
                .alpha(if (uiState.isSearchable) 1f else 0.6f),
            enabled = uiState.isSearchable,
            icon = null,
            placeholder = {
                Text(
                    text = stringResource(BaseR.string.search_online_posts),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            onSearch = {
                onSearch()
            },
        )

        Spacer(modifier = Modifier.width(8.dp))

        ServiceDropdownButton(
            onSelectService = onSelectService,
            onServiceManagementClick = onManagementClick,
            onLongClickCurrentService = {  },
            services = ImmutableHolder(uiState.services),
            currentService = uiState.currentService,
        )
    }
}
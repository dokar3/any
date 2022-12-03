package any.ui.runsql

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.compose.ImmutableHolder
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.AnimatedPopupItem
import any.ui.common.widget.rememberAnimatedPopupDismissRequester

@Composable
internal fun DbSelector(
    dbs: ImmutableHolder<List<Db>>,
    selectedDb: Db,
    onSelect: (Db) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        var showSelector by remember { mutableStateOf(false) }

        Button(
            onClick = { showSelector = true },
            modifier = Modifier.widthIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
            ),
            elevation = null,
            shape = CircleShape,
        ) {
            Text(
                text = selectedDb.name,
                fontSize = 14.sp,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        if (showSelector) {
            val dismissRequester = rememberAnimatedPopupDismissRequester()
            AnimatedPopup(
                dismissRequester = dismissRequester,
                onDismissed = { showSelector = false },
                scaleAnimOrigin = TransformOrigin(0.5f, 0f),
                contentAlignmentToAnchor = Alignment.Center,
            ) {
                for ((idx, db) in dbs.value.withIndex()) {
                    AnimatedPopupItem(
                        index = idx,
                        onClick = {
                            if (db != selectedDb) {
                                onSelect(db)
                            }
                            dismissRequester.dismiss()
                        },
                    ) {
                        Text(db.name)
                    }
                }
            }
        }
    }
}

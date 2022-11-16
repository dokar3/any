package any.ui.service

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import any.ui.common.theme.placeholder

@Composable
internal fun ListViewPlaceholder(
    modifier: Modifier = Modifier,
    itemCount: Int = 3,
    color: Color = MaterialTheme.colors.placeholder,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(itemCount) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(5f / 4f)
                        .background(color)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(color)
                )
            }
        }
    }
}

@Composable
internal fun GridViewPlaceholder(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.placeholder,
    rows: Int = 2,
    columns: Int = 2,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(columns) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
internal fun FullWidthViewPlaceholder(
    modifier: Modifier = Modifier,
    itemCount: Int = 1,
    color: Color = MaterialTheme.colors.placeholder,
) {
    Column(modifier = modifier.fillMaxSize()) {
        repeat(itemCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = color,
                            shape = CircleShape,
                        )
                )

                Spacer(modifier = Modifier.width(16.dp))

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(color)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(color)
            )
        }
    }
}

@Composable
internal fun CardViewPlaceholder(
    modifier: Modifier = Modifier,
    itemCount: Int = 1,
    color: Color = MaterialTheme.colors.placeholder,
) {
    Column(modifier = modifier.fillMaxSize()) {
        repeat(itemCount) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 2.dp,
                        color = color,
                        shape = MaterialTheme.shapes.medium,
                    ),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(color)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(color)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = color,
                                    shape = CircleShape,
                                )
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

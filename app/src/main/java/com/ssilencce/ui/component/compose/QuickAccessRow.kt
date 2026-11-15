package com.ssilencce.ui.component.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssilencce.core.model.action.QuickAccessAction

/**
 * Horizontal row of user-selected Now Playing shortcuts.
 */
@Composable
fun QuickAccessRow(
    actions: List<QuickAccessAction>,
    onAction: (QuickAccessAction) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .92f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (actions.isEmpty()) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
    ) {
        Row(
            modifier =
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            actions.forEach { action ->
                IconButton(onClick = { onAction(action) }) {
                    Icon(
                        painter = painterResource(action.iconRes),
                        contentDescription = stringResource(action.titleRes),
                        tint = contentColor,
                    )
                }
            }
        }
    }
}

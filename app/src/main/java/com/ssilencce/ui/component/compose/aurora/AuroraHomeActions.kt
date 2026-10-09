package com.ssilencce.ui.component.compose.aurora

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssilencce.R
import com.ssilencce.ui.theme.AuroraDesign

@Composable
fun AuroraHomeActions(
    songCount: String,
    onTopTracks: () -> Unit,
    onLastAdded: () -> Unit,
    onHistory: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile(R.drawable.ic_trending_up_24dp, R.string.top_tracks_label, onTopTracks, Modifier.weight(1f))
            ActionTile(R.drawable.ic_album_24dp, R.string.last_added_label, onLastAdded, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile(R.drawable.ic_history_24dp, R.string.history_label, onHistory, Modifier.weight(1f))
            ActionTile(R.drawable.ic_shuffle_24dp, R.string.shuffle_action, onShuffle, Modifier.weight(1f), songCount)
        }
    }
}

@Composable
private fun ActionTile(
    icon: Int,
    label: Int,
    onClick: () -> Unit,
    modifier: Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AuroraDesign.glass)
            .border(1.dp, AuroraDesign.border, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 66.dp)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).background(Color.White.copy(alpha = .08f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = AuroraDesign.foreground)
            if (subtitle != null) Text(subtitle, Modifier.padding(top = 2.dp), fontSize = 11.5.sp, color = AuroraDesign.muted)
        }
    }
}

@Preview(widthDp = 392)
@Composable
private fun AuroraHomeActionsPreview() {
    AuroraHomeActions("", {}, {}, {}, {})
}

package com.mawaqit.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mawaqit.app.R
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.ui.theme.SuccessGreen
import com.mawaqit.app.util.PrayerStatus

/**
 * One prayer row (DESIGN.md §6 AgendaRow adapted + Settings switch row):
 * status icon · name · time. Tap anywhere on the row toggles
 * the prayed checkmark (PHASE_4 guidebook: "Tapping a prayer marks it as prayed").
 * Alarm toggles moved to Settings (PHASE-8.1).
 *
 * [prayed] comes from the salah_log (authoritative); [status] is the clock
 * classification used only for the CURRENT highlight when not yet prayed.
 */
@Composable
fun PrayerRow(
    prayer: PrayerName,
    timeStr: String,
    status: PrayerStatus,
    prayed: Boolean,
    onTogglePrayed: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrent = status == PrayerStatus.CURRENT && !prayed
    val nameColor = when {
        prayed -> MaterialTheme.colorScheme.outline
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTogglePrayed(!prayed) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prayed) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.content_desc_marked_prayed),
                tint = SuccessGreen,
                modifier = Modifier.size(26.dp)
            )
        } else {
            // Not prayed: a real ring instead of the thin hairline that used to sit next
            // to the solid green check (they balanced badly). Ring weight and colour follow
            // DESIGN.md §9's chip treatment — muted for "not done", gold for the CURRENT
            // prayer so the eye is drawn to the one you can act on right now.
            val notPrayedDesc = stringResource(R.string.content_desc_not_prayed)
            val ringWeight = if (isCurrent) 2.5.dp else 1.5.dp
            val ringColor =
                if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .border(width = ringWeight, color = ringColor, shape = CircleShape)
                    .semantics { contentDescription = notPrayedDesc }
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = PrayerNameLabel(prayer),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = nameColor
            )
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.current_prayer_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = timeStr,
            style = MaterialTheme.typography.bodyLarge,
            color = if (prayed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        )
    }
}

package com.mawaqit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mawaqit.app.R
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.ui.theme.SuccessGreen
import com.mawaqit.app.util.PrayerStatus

/**
 * One prayer row (DESIGN.md §6 AgendaRow adapted + Settings switch row):
 * status icon · name · time · alarm toggle. Tap anywhere on the row toggles
 * the prayed checkmark (PHASE_4 guidebook: "Tapping a prayer marks it as prayed").
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
    alarmEnabled: Boolean,
    alarmSwitchEnabled: Boolean,
    onTogglePrayed: (Boolean) -> Unit,
    onToggleAlarm: (Boolean) -> Unit,
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
        Icon(
            imageVector = if (prayed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (prayed) {
                stringResource(R.string.content_desc_marked_prayed)
            } else {
                stringResource(R.string.content_desc_not_prayed)
            },
            tint = if (prayed) SuccessGreen else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(28.dp)
        )
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
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = alarmEnabled,
            onCheckedChange = onToggleAlarm,
            enabled = alarmSwitchEnabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

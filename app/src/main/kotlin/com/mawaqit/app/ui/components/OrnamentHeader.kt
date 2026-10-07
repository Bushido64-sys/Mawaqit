package com.mawaqit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.mawaqit.app.ui.theme.PrimaryGold

/** ── ✦ ── centered gold star with horizontal rules (PHASE-9.1 header ornament). */
@Composable
fun OrnamentHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )
        Spacer(Modifier.width(12.dp))
        Text("✦", color = PrimaryGold)
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Box(
            Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )
    }
}

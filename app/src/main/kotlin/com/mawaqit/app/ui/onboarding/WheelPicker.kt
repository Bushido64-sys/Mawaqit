package com.mawaqit.app.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.mawaqit.app.ui.theme.PrimaryGold
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment

/**
 * Wheel-style picker (iOS/Android alarm-clock pattern): centered item is the
 * selected one; simpler than dropdowns for short-to-medium lists and familiar
 * from system time pickers. onSettled fires only when scrolling stops on a page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSettled: (Int) -> Unit
) {
    if (items.isEmpty()) {
        Text("—", color = MaterialTheme.colorScheme.outline, modifier = modifier)
        return
    }
    val state = rememberPagerState(pageCount = { items.size })
    // Only save a selection after the user actually scrolls the wheel —
    // firing on composition would auto-save the first row and clobber any
    // GPS location the user just picked (user bug, PHASE-9.2).
    var touched by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(state.currentPage, state.isScrollInProgress) {
        if (state.isScrollInProgress) touched = true
        if (!state.isScrollInProgress && touched && enabled && items.isNotEmpty()) onSettled(state.currentPage)
    }
    androidx.compose.foundation.layout.Box(modifier) {
        VerticalPager(
            state = state,
            pageSize = androidx.compose.foundation.pager.PageSize.Fixed(44.dp),
            userScrollEnabled = enabled,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 88.dp),
            modifier = Modifier.height(220.dp)
        ) { i ->
            val center = i == state.currentPage
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    items[i],
                    fontSize = if (center) 18.sp else 15.sp,
                    color = if (center) PrimaryGold else MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
        // Subtle center band to mark the selected row
        androidx.compose.foundation.Canvas(
            Modifier.align(androidx.compose.ui.Alignment.Center).fillMaxWidth().height(44.dp)
        ) {
            val y1 = 0f
            val y2 = size.height
            drawLine(color = PrimaryGold, start = androidx.compose.ui.geometry.Offset(0f, y1), end = androidx.compose.ui.geometry.Offset(size.width, y1), strokeWidth = 2f)
            drawLine(color = PrimaryGold, start = androidx.compose.ui.geometry.Offset(0f, y2), end = androidx.compose.ui.geometry.Offset(size.width, y2), strokeWidth = 2f)
        }
    }
}

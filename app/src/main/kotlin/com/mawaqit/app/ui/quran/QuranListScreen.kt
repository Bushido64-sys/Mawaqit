package com.mawaqit.app.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaqit.app.R
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.data.repository.QuranProgress
import com.mawaqit.app.ui.components.ErrorState
import com.mawaqit.app.ui.components.LoadingState
import com.mawaqit.app.ui.theme.ChipBg
import com.mawaqit.app.ui.theme.ChipText
import com.mawaqit.app.ui.theme.NotoNaskhArabicFamily
import com.mawaqit.app.ui.theme.PrimaryBlue
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SurfaceDeep
import com.mawaqit.app.ui.theme.TextMuted
import com.mawaqit.app.ui.theme.TextPrimary

/**
 * PHASE_6 Surah list — DESIGN.md Screen 4: top bar title + inline search,
 * white rows with a blue-tint number box, English name + meaning, Arabic
 * name right-aligned (RTL, Rule 13).
 */
@Composable
fun QuranListScreen(
    onSurahClick: (Int) -> Unit,
    viewModel: QuranListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.quranProgress.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.al_quran),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))

        // PHASE-6.3 — Continue Reading card above the search bar; hidden
        // entirely until the user has actually read something.
        progress?.let { QuranProgressCard(progress = it, onContinueClick = onSurahClick) }
        Spacer(Modifier.height(12.dp))

        SearchBar(
            query = state.searchQuery,
            active = state.isSearchActive,
            onQueryChange = viewModel::onQueryChange,
            onActiveChange = viewModel::onSearchActiveChange
        )

        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> LoadingState()
            state.error -> ErrorState(
                message = stringResource(R.string.error_content_unavailable),
                onRetry = viewModel::refresh
            )
            state.surahs.isEmpty() -> EmptySearchState()
            else -> SurahList(
                surahs = state.surahs,
                completed = state.completedNumbers,
                onSurahClick = onSurahClick
            )
        }
    }
}

/** Rounded-16dp search field (DESIGN.md §3 input field spec), always visible. */
@Composable
private fun SearchBar(
    query: String,
    active: Boolean,
    onQueryChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, PrimaryGold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(8.dp))
        androidx.compose.material3.TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_surah)) },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty() || active) {
                    IconButton(onClick = {
                        onQueryChange("")
                        onActiveChange(false)
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                }
            },
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptySearchState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.error_content_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

/**
 * One Surah row: number chip (blue tint) · English + meaning · Arabic right.
 * PHASE-6.3 — a completed surah is highlighted in gold: tinted row, gold
 * border + gold text on the number chip, and a "✓ Completed" tag.
 */
@Composable
private fun SurahRow(surah: SurahEntity, isCompleted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCompleted) PrimaryGold.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surface
            )
            .then(
                if (isCompleted) {
                    Modifier.border(1.dp, PrimaryGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number box — blue chip; gold border + gold text when completed
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ChipBg)
                .then(
                    if (isCompleted) {
                        Modifier.border(1.dp, PrimaryGold, RoundedCornerShape(12.dp))
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.number.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) PrimaryGold else ChipText
            )
        }
        Spacer(Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah.nameEnglish,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${surah.nameMeaning} · ${surah.numberOfAyahs} ${
                    stringResource(R.string.verses)
                }",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Arabic name — RTL, right side (Rule 13); "✓ Completed" tag beneath
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = surah.nameArabic,
                fontFamily = NotoNaskhArabicFamily,
                fontSize = 18.sp,
                textAlign = TextAlign.End,
                color = TextPrimary
            )
            if (isCompleted) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "✓ ${stringResource(R.string.progress_completed)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
            }
        }
    }
}

@Composable
private fun SurahList(
    surahs: List<SurahEntity>,
    completed: Set<Int>,
    onSurahClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
    ) {
        items(surahs, key = { it.number }) { surah ->
            SurahRow(
                surah = surah,
                isCompleted = surah.number in completed,
                onClick = { onSurahClick(surah.number) }
            )
        }
    }
}

/**
 * PHASE-6.3 — the Continue Reading card: deep blue with a gold border,
 * this-surah + whole-Quran gold progress bars, and a gold "Continue"
 * button that reopens the reader at the last-read page.
 */
@Composable
private fun QuranProgressCard(
    progress: QuranProgress,
    onContinueClick: (Int) -> Unit
) {
    val surah = progress.continueSurah ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDeep)
            .border(1.dp, PrimaryGold.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.progress_continue),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryGold
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.progress_continue_body,
                    surah.nameEnglish,
                    surah.furthestAyah,
                    surah.totalAyahs
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = surah.nameArabic,
                fontFamily = NotoNaskhArabicFamily,
                fontSize = 18.sp,
                textAlign = TextAlign.End,
                color = Color.White
            )
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(label = surah.nameEnglish, percent = surah.percent)
        Spacer(Modifier.height(10.dp))
        ProgressBar(
            label = stringResource(R.string.progress_quran_total),
            percent = progress.percent
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(PrimaryGold.copy(alpha = 0.18f))
                .border(1.dp, PrimaryBlue, RoundedCornerShape(50))
                .clickable { onContinueClick(surah.surahNumber) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.progress_continue),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/** Thin gold progress bar: dark track, gold fill, % label row above. */
@Composable
private fun ProgressBar(label: String, percent: Int) {
    Column {
        Row {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PrimaryGold)
            )
        }
    }
}

package com.mawaqit.app.ui.quran

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaqit.app.R
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.ui.components.ErrorState
import com.mawaqit.app.ui.components.LoadingState
import com.mawaqit.app.ui.theme.ChipBg
import com.mawaqit.app.ui.theme.ChipText
import com.mawaqit.app.ui.theme.NotoNaskhArabicFamily
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

/** One Surah row: number chip (blue tint) · English + meaning · Arabic right. */
@Composable
private fun SurahRow(surah: SurahEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number box — light blue chip fill, blue text (DESIGN.md §1 #7)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ChipBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.number.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ChipText
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

        // Arabic name — RTL, right side (Rule 13)
        Text(
            text = surah.nameArabic,
            fontFamily = NotoNaskhArabicFamily,
            fontSize = 18.sp,
            textAlign = TextAlign.End,
            color = TextPrimary
        )
    }
}

@Composable
private fun SurahList(surahs: List<SurahEntity>, onSurahClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
    ) {
        items(surahs, key = { it.number }) { surah ->
            SurahRow(surah = surah, onClick = { onSurahClick(surah.number) })
        }
    }
}

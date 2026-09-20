package com.mawaqit.app.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaqit.app.R
import com.mawaqit.app.data.db.AyahEntity
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.ui.components.ErrorState
import com.mawaqit.app.ui.components.LoadingState
import com.mawaqit.app.ui.components.VideoBackground
import com.mawaqit.app.ui.theme.AmiriFontFamily
import com.mawaqit.app.ui.theme.InterFontFamily
import com.mawaqit.app.ui.theme.NotoNaskhArabicFamily
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.util.QuranText
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * PHASE-6.1 Surah reading screen — "The Mushaf Card".
 *
 * User-directed redesign of PHASE_6: the surah no longer scrolls end-to-end;
 * it is chunked into page-sized cards (QuranText.chunkIntoPages, never splits
 * an ayah) shown in a HorizontalPager — swipe = page turn. Each card:
 * deep-navy surface, 28dp radius, the app's ONE ceremonial gold border.
 * Header: Arabic name (gold) + English · meaning · verses-range footer.
 * Translation switch is a segmented pill in the card header (kit pattern);
 * font size lives behind the "Aa" chip (bottom sheet, persists to Phase 8).
 * Bismillah iff the API's bismillah_pre == true (API_REFERENCE.md 2.2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    onBack: () -> Unit,
    viewModel: SurahDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFontSheet by remember { mutableStateOf(false) }

    VideoBackground(overlayAlpha = 0.7f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopBar(
                title = state.surah?.nameEnglish.orEmpty(),
                onBack = onBack,
                onFontSize = { showFontSheet = true }
            )

            when {
                state.isLoading -> LoadingState()
                state.error -> ErrorState(
                    message = stringResource(R.string.error_content_unavailable),
                    onRetry = viewModel::load
                )
                else -> {
                    SegmentedModeToggle(
                        selected = state.displayMode,
                        onSelect = viewModel::setMode
                    )
                    MushafPager(
                        state = state,
                        onFirstSwipe = viewModel::onFirstSwipe
                    )
                }
            }
        }
    }

    if (showFontSheet) {
        FontSizeSheet(
            selected = state.fontScale,
            onSelect = viewModel::setFontScale,
            onDismiss = { showFontSheet = false }
        )
    }
}

/** DESIGN.md §4 top bar: circular chip back arrow, centered title — plus "Aa". */
@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
    onFontSize: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_desc_back),
                tint = Color.White
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(onClick = onFontSize),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * PHASE-6.1 translation toggle — compact segmented pill with a sliding gold
 * selector (DESIGN.md §8 "sliding filled-pill selector"), replacing the
 * three fat pills the user rejected. Shows what renders BESIDES the Arabic.
 */
@Composable
private fun SegmentedModeToggle(
    selected: DisplayMode,
    onSelect: (DisplayMode) -> Unit
) {
    val options = listOf(
        DisplayMode.ARABIC_ONLY to "العربية",
        DisplayMode.ARABIC_ENGLISH to "EN",
        DisplayMode.ARABIC_URDU to "اردو"
    )
    BoxWithConstraints(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 8.dp)
            .fillMaxWidth(0.72f)
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 3
        val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
        val indicatorX by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(250),
            label = "segmentIndicator"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(segmentWidth)
                .height(32.dp)
                .background(PrimaryGold, RoundedCornerShape(50))
        )
        Row(modifier = Modifier.height(32.dp)) {
            options.forEach { (mode, label) ->
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (mode == selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (mode == selected) Color(0xFF071E35) else Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Pager of mushaf cards + first-visit swipe hint. */
@Composable
private fun MushafPager(
    state: SurahDetailUiState,
    onFirstSwipe: () -> Unit
) {
    val pages = state.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Any horizontal drag → the swipe hint retires forever (ViewModel guards).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPageOffsetFraction }
            .distinctUntilChanged()
            .collect { fraction -> if (fraction != 0f) onFirstSwipe() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            MushafPage(
                surah = state.surah,
                pageAyahs = pages[page],
                pageIndex = page,
                pageCount = pages.size,
                mode = state.displayMode,
                fontScale = state.fontScale.multiplier,
                showBismillah = page == 0 && state.bismillahPre
            )
        }

        AnimatedVisibility(
            visible = state.showSwipeHint,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "👇 ${stringResource(R.string.reader_swipe_hint)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * One mushaf card: gold-framed deep-navy block, header inside, ayahs
 * center-staged, verses-range + page dots footer.
 */
@Composable
private fun MushafPage(
    surah: SurahEntity?,
    pageAyahs: List<AyahEntity>,
    pageIndex: Int,
    pageCount: Int,
    mode: DisplayMode,
    fontScale: Float,
    showBismillah: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF071E35).copy(alpha = 0.55f))
                .border(1.dp, PrimaryGold.copy(alpha = 0.75f), RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ── Header: Arabic name (gold) + English · meaning ──
            if (surah != null) {
                Text(
                    text = surah.nameArabic,
                    fontFamily = AmiriFontFamily,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    color = PrimaryGold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${surah.nameEnglish} · ${surah.nameMeaning}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PrimaryGold.copy(alpha = 0.35f), RoundedCornerShape(50))
                )
                Spacer(Modifier.height(10.dp))
            }

            if (showBismillah) {
                RtlText(
                    text = stringResource(R.string.bismillah),
                    fontSize = (20 * fontScale).sp,
                    lineHeight = (34 * fontScale).sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Ayahs: centered when short, scrollable inside the card when tall ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    pageAyahs.forEach { ayah ->
                        AyahBlock(ayah = ayah, mode = mode, fontScale = fontScale)
                    }
                }
            }

            // ── Footer: verses range + page dots ──
            if (pageAyahs.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.reader_page_verses,
                        pageAyahs.first().ayahNumber,
                        pageAyahs.last().ayahNumber
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            if (pageCount > 1) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val active = index == pageIndex
                        Box(
                            modifier = Modifier
                                .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (active) PrimaryGold else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahBlock(
    ayah: AyahEntity,
    mode: DisplayMode,
    fontScale: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Arabic + ornate marker — center-staged mushaf style, bundled Amiri.
        RtlText(
            text = "${ayah.arabicText} ${QuranText.arabicMarker(ayah.ayahNumber)}",
            fontSize = (22 * fontScale).sp,
            lineHeight = (38 * fontScale).sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        when (mode) {
            DisplayMode.ARABIC_ONLY -> Unit
            DisplayMode.ARABIC_ENGLISH -> Text(
                text = QuranText.stripFootnoteMarkers(ayah.translationEn),
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
            DisplayMode.ARABIC_URDU -> RtlText(
                text = QuranText.stripFootnoteMarkers(ayah.translationUr),
                fontSize = (16 * fontScale).sp,
                lineHeight = (26 * fontScale).sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Rule 13: Arabic/Urdu text renders inside an RTL composition locally.
 * Bundled Amiri first (offline Quran calligraphy), downloadable Noto fallback.
 */
@Composable
private fun RtlText(
    text: String,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.End
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = text,
            fontFamily = NotoNaskhArabicFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            textAlign = textAlign,
            color = color,
            modifier = modifier.fillMaxWidth()
        )
    }
}

/** "Aa" bottom sheet — S/M/L/XL with live Bismillah preview (Alerts-2 pattern). */
@Composable
private fun FontSizeSheet(
    selected: ReaderFontScale,
    onSelect: (ReaderFontScale) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0c2b45)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.reader_font_size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReaderFontScale.entries.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) PrimaryGold else Color.White.copy(alpha = 0.12f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) PrimaryGold
                                else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onSelect(option) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF071E35) else Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            // Live preview — resizes the moment a size is tapped.
            RtlText(
                text = stringResource(R.string.bismillah),
                fontSize = (20 * selected.multiplier).sp,
                lineHeight = (34 * selected.multiplier).sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

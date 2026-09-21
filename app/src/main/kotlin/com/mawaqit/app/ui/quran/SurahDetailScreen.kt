package com.mawaqit.app.ui.quran

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.mawaqit.app.ui.theme.PrimaryBlue
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.util.QuranText
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * PHASE-6.2 Surah reading screen — "The Perfect Page".
 *
 * Pages are fit to the REAL screen via [FitPages] (TextMeasurer) — the card
 * never scrolls; swiping is the only navigation. The ☰ chip opens a
 * "Reading Settings" sheet (translation mode + text size). Page dots are a
 * sliding window of 7 plus an explicit "Page X of Y · Verses a–b" line.
 *
 * PHASE-6.4 "Only the Button Counts":
 * - Progress is written by EXACTLY one event: tapping "Mark as read".
 *   Swiping and opening record nothing — browsing can never move the
 *   Continue card.
 * - Button states: unread page → outlined "Mark as read"; tapped → solid
 *   gold "Read ✓" (tap again on a read page re-pins the resume point).
 * - The old text swipe hint is replaced by a two-step coach mark
 *   (DESIGN.md pop-up style): ① swipe lesson (non-blocking — the user can
 *   swipe straight through it) → ② "Mark as read" lesson. One prefs flag
 *   retires both forever.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    onBack: () -> Unit,
    viewModel: SurahDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettingsSheet by remember { mutableStateOf(false) }

    VideoBackground(overlayAlpha = 0.7f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopBar(
                title = state.surah?.nameEnglish.orEmpty(),
                onBack = onBack,
                onSettings = { showSettingsSheet = true }
            )

            when {
                state.isLoading -> LoadingState()
                state.error -> ErrorState(
                    message = stringResource(R.string.error_content_unavailable),
                    onRetry = viewModel::load
                )
                else -> MushafPager(
                    state = state,
                    onCoachAdvance = viewModel::onCoachAdvance,
                    onCoachDone = viewModel::onCoachDone,
                    onMarkClick = viewModel::markPage,
                    onConsumeResume = viewModel::consumeResumeToast
                )
            }
        }
    }

    if (showSettingsSheet) {
        ReaderSettingsSheet(
            selected = state.displayMode,
            onSelectMode = viewModel::setMode,
            fontScale = state.fontScale,
            onSelectFont = viewModel::setFontScale,
            onDismiss = { showSettingsSheet = false }
        )
    }
}

/** DESIGN.md §4 top bar: circular back chip, centered title, circular ☰ chip. */
@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
    onSettings: () -> Unit
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
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.content_desc_reader_settings),
                tint = Color.White
            )
        }
    }
}

/** Pager of fitted mushaf pages + resume logic + PHASE-6.4 coach marks. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MushafPager(
    state: SurahDetailUiState,
    onCoachAdvance: () -> Unit,
    onCoachDone: () -> Unit,
    onMarkClick: (Int, Int) -> Unit,
    onConsumeResume: () -> Unit
) {
    val bismillah = stringResource(R.string.bismillah)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val context = LocalContext.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // PHASE-6.2 — measure-then-pack: pages fit THIS screen exactly,
        // re-fitted whenever font size / translation mode / content changes.
        val footerExtraPx = with(density) { FitPages.MUSHAF_FOOTER_H.roundToPx() }
        val fittedPages = remember(
            state.ayahs, state.surah, state.displayMode, state.fontScale,
            state.bismillahPre, bismillah, measurer, density,
            constraints.maxWidth, constraints.maxHeight, footerExtraPx
        ) {
            FitPages.fit(
                ayahs = state.ayahs,
                surah = state.surah,
                mode = state.displayMode,
                fontScale = state.fontScale.multiplier,
                bismillahText = bismillah,
                showBismillah = state.bismillahPre,
                measurer = measurer,
                density = density,
                maxWidthPx = constraints.maxWidth,
                maxHeightPx = constraints.maxHeight,
                footerExtraHeight = footerExtraPx
            )
        }
        val pagerState = rememberPagerState(pageCount = { fittedPages.size })

        // PHASE-6.3 — resume: jump straight to the stored page (legacy pin or
        // last MARKED page) once pages exist; toast once, then retire it.
        LaunchedEffect(fittedPages.size, state.resumeAyah) {
            val target = state.resumeAyah ?: return@LaunchedEffect
            if (fittedPages.isEmpty()) return@LaunchedEffect
            val page = fittedPages.indexOfFirst { it.ayahs.firstOrNull()?.ayahNumber == target }
            if (page >= 0) {
                pagerState.scrollToPage(page)
                Toast.makeText(
                    context,
                    context.getString(R.string.reader_resumed, page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            }
            onConsumeResume()
        }

        // PHASE-6.4 coach step 1 — any real swipe advances the lesson. Nothing
        // is recorded anymore (button-only progress); this only teaches.
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPageOffsetFraction }
                .distinctUntilChanged()
                .collect { fraction -> if (fraction != 0f) onCoachAdvance() }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val fitted = fittedPages.getOrNull(page) ?: return@HorizontalPager
                MushafPage(
                    surah = state.surah,
                    page = fitted,
                    pageIndex = page,
                    pageCount = fittedPages.size,
                    mode = state.displayMode,
                    fontScale = state.fontScale.multiplier,
                    showBismillah = page == 0 && state.bismillahPre,
                    furthestAyah = state.furthestAyah,
                    onMarkClick = onMarkClick
                )
            }

            // PHASE-6.4 — two-step coach mark replaces the old text hint.
            state.coachStep?.let { step ->
                CoachOverlay(
                    step = step,
                    onAdvance = onCoachAdvance,
                    onDone = onCoachDone
                )
            }
        }
    }
}

/**
 * PHASE-6.4 coach mark (DESIGN.md pop-up style): deep-navy card, gold border,
 * dot pagination. The scrim does NOT intercept touches — the reader beneath
 * stays usable, so the user can literally swipe through lesson ① and press
 * the real button through lesson ②. "Got it" (or completing the action)
 * advances/retires the coach.
 */
@Composable
private fun CoachOverlay(
    step: Int,
    onAdvance: () -> Unit,
    onDone: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "coachSwipe")
    val arrowX by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coachSwipeArrow"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Visual-only scrim — no pointerInput, so touches pass through.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0c2b45))
                .border(1.dp, PrimaryGold.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (step == 1) {
                Text(
                    text = stringResource(R.string.coach_swipe_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.coach_swipe_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                // Animated finger: slides left→right, the actual gesture to make.
                Text(
                    text = "👇→",
                    fontSize = 28.sp,
                    modifier = Modifier.offset(x = arrowX.dp)
                )
                Spacer(Modifier.height(14.dp))
                CoachGotIt(onClick = onAdvance)
            } else {
                Text(
                    text = stringResource(R.string.coach_button_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.coach_button_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                // Mini replica of the exact pill to look for.
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PrimaryGold.copy(alpha = 0.18f))
                        .border(1.dp, PrimaryBlue, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.reader_mark_as_read),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(14.dp))
                CoachGotIt(onClick = onDone)
            }
        }
    }
}

/** Gold "Got it" pill — the coach's only interactive element. */
@Composable
private fun CoachGotIt(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PrimaryGold)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.coach_got_it),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF071E35)
        )
    }
}

/**
 * One mushaf card: gold-framed deep-navy block, header inside, ayahs
 * center-staged, footer with explicit page position + sliding-window dots,
 * then the PHASE-6.4 "Mark as read" strip. The ayah column scrolls ONLY
 * when FitPages flagged the page oversized.
 */
@Composable
private fun MushafPage(
    surah: SurahEntity?,
    page: FitPages.FittedPage,
    pageIndex: Int,
    pageCount: Int,
    mode: DisplayMode,
    fontScale: Float,
    showBismillah: Boolean,
    furthestAyah: Int,
    onMarkClick: (Int, Int) -> Unit
) {
    val pageAyahs = page.ayahs
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FitPages.PAGE_PAD_H, vertical = FitPages.PAGE_PAD_V)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF071E35).copy(alpha = 0.55f))
                .border(1.dp, PrimaryGold.copy(alpha = 0.75f), RoundedCornerShape(28.dp))
                .padding(horizontal = FitPages.CARD_PAD_H, vertical = FitPages.CARD_PAD_V)
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

            // ── Ayahs: center-staged; scroll only on the rare oversized page ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = if (page.allowScroll) {
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    pageAyahs.forEach { ayah ->
                        AyahBlock(ayah = ayah, mode = mode, fontScale = fontScale)
                    }
                }
            }

            // ── Footer: position line + sliding-window dots + mark strip ──
            if (pageAyahs.isNotEmpty()) {
                val verses = if (pageCount > 1) {
                    stringResource(
                        R.string.reader_page_verses,
                        pageAyahs.first().ayahNumber,
                        pageAyahs.last().ayahNumber
                    )
                } else ""
                val pageLine = if (pageCount > 1) {
                    stringResource(R.string.reader_page_position, pageIndex + 1, pageCount, verses)
                } else {
                    verses
                }
                Text(
                    text = pageLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                if (pageCount > 1) {
                    Spacer(Modifier.height(10.dp))
                    WindowedPageDots(
                        pageCount = pageCount,
                        pageIndex = pageIndex,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(10.dp))

                // PHASE-6.4 — the ONLY progress writer (fixed height; FitPages
                // reserves exactly FitPages.MUSHAF_FOOTER_H for it).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FitPages.MUSHAF_FOOTER_H),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MarkAsReadButton(
                        isRead = furthestAyah >= pageAyahs.last().ayahNumber,
                        onClick = {
                            onMarkClick(
                                pageAyahs.first().ayahNumber,
                                pageAyahs.last().ayahNumber
                            )
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.reader_page_short, pageIndex + 1, pageCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * PHASE-6.4 — the one button that counts. Unread page → gold-shaded pill
 * with a blue border, "Mark as read". Page read → solid gold, "Read ✓"
 * (tapping again simply re-pins the resume point to this page).
 */
@Composable
private fun MarkAsReadButton(
    isRead: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isRead) PrimaryGold else PrimaryGold.copy(alpha = 0.18f))
            .border(1.dp, PrimaryBlue, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = if (isRead) Color(0xFF071E35) else Color.White,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(
                if (isRead) R.string.reader_marked else R.string.reader_mark_as_read
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isRead) Color(0xFF071E35) else Color.White
        )
    }
}

/**
 * PHASE-6.2 fix #3 — sliding window of max [WINDOW] dots, active dot centered,
 * so page 41 of 41 still shows its gold dot (the old full-row overflow bug).
 */
private const val DOTS_WINDOW = 7

@Composable
private fun WindowedPageDots(
    pageCount: Int,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val window = DOTS_WINDOW.coerceAtMost(pageCount)
    val start = (pageIndex - window / 2).coerceIn(0, (pageCount - window).coerceAtLeast(0))
    val active = pageIndex - start
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(window) { index ->
            val isActive = index == active
            Box(
                modifier = Modifier
                    .size(width = if (isActive) 18.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isActive) PrimaryGold else Color.White.copy(alpha = 0.35f)
                    )
            )
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

/**
 * PHASE-6.2 — "Reading Settings" sheet behind the ☰ chip: translation mode
 * (sliding gold selector) + text size (S/M/L/XL) live together. The page
 * itself stays pure text; Phase 8's Settings screen will read the same prefs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    selected: DisplayMode,
    onSelectMode: (DisplayMode) -> Unit,
    fontScale: ReaderFontScale,
    onSelectFont: (ReaderFontScale) -> Unit,
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
            // ── Translation mode ──
            Text(
                text = stringResource(R.string.reader_translation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            SegmentedModeToggle(
                selected = selected,
                onSelect = onSelectMode,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))

            // ── Text size ──
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
                    val isSelected = option == fontScale
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
                            .clickable { onSelectFont(option) }
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
                fontSize = (20 * fontScale.multiplier).sp,
                lineHeight = (34 * fontScale.multiplier).sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Translation toggle — compact segmented pill with a sliding gold selector
 * (DESIGN.md §8). Lives in the Reading Settings sheet (PHASE-6.2), not on
 * the page. Shows what renders BESIDES the always-on Arabic.
 */
@Composable
private fun SegmentedModeToggle(
    selected: DisplayMode,
    onSelect: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        DisplayMode.ARABIC_ONLY to "العربية",
        DisplayMode.ARABIC_ENGLISH to "EN",
        DisplayMode.ARABIC_URDU to "اردو"
    )
    BoxWithConstraints(
        modifier = modifier
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

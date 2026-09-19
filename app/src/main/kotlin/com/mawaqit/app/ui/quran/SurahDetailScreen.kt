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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.mawaqit.app.ui.components.ErrorState
import com.mawaqit.app.ui.components.LoadingState
import com.mawaqit.app.ui.components.VideoBackground
import com.mawaqit.app.ui.theme.NotoNaskhArabicFamily
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.util.QuranText

/**
 * PHASE_6 Surah reading screen — DESIGN.md Screen 5: video background
 * (gradient fallback until assignment/06 lands), transparent top bar, all
 * text white. Arabic/Urdu RTL 22sp/16sp Naskh (Rule 13); English LTR 14sp.
 * Bismillah header iff the API's bismillah_pre == true (no hardcoded
 * Surah-9 exception — API_REFERENCE.md Endpoint 2.2).
 */
@Composable
fun SurahDetailScreen(
    onBack: () -> Unit,
    viewModel: SurahDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    VideoBackground(overlayAlpha = 0.7f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopBar(
                title = state.surah?.nameEnglish.orEmpty(),
                onBack = onBack
            )

            when {
                state.isLoading -> LoadingState()
                state.error -> ErrorState(
                    message = stringResource(R.string.error_content_unavailable),
                    onRetry = viewModel::load
                )
                else -> {
                    ModeToggle(
                        selected = state.displayMode,
                        onSelect = viewModel::setMode
                    )
                    SurahContent(state = state)
                }
            }
        }
    }
}

/** DESIGN.md §4 top bar: white circular chip back arrow, centered title. */
@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
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
    }
}

/** 3-way language toggle — gold pill when selected (kit CTA pattern). */
@Composable
private fun ModeToggle(
    selected: DisplayMode,
    onSelect: (DisplayMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModePill(
            label = stringResource(R.string.mode_arabic),
            selected = selected == DisplayMode.ARABIC_ONLY,
            onClick = { onSelect(DisplayMode.ARABIC_ONLY) },
            modifier = Modifier.weight(1f)
        )
        ModePill(
            label = stringResource(R.string.mode_arabic_english),
            selected = selected == DisplayMode.ARABIC_ENGLISH,
            onClick = { onSelect(DisplayMode.ARABIC_ENGLISH) },
            modifier = Modifier.weight(1f)
        )
        ModePill(
            label = stringResource(R.string.mode_arabic_urdu),
            selected = selected == DisplayMode.ARABIC_URDU,
            onClick = { onSelect(DisplayMode.ARABIC_URDU) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) PrimaryGold else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun SurahContent(state: SurahDetailUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, bottom = 32.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Arabic name (big) + English · meaning · revelation
        item {
            Spacer(Modifier.height(8.dp))
            val surah = state.surah
            if (surah != null) {
                Text(
                    text = surah.nameArabic,
                    fontFamily = NotoNaskhArabicFamily,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    color = PrimaryGold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${surah.nameMeaning} · ${
                        stringResource(
                            if (surah.revelationType == "Medinan") R.string.medinan
                            else R.string.meccan
                        )
                    } · ${surah.numberOfAyahs} ${stringResource(R.string.verses)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }
            if (state.bismillahPre) {
                Text(
                    text = stringResource(R.string.bismillah),
                    fontFamily = NotoNaskhArabicFamily,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        items(state.ayahs, key = { it.ayahNumber }) { ayah ->
            AyahBlock(ayah = ayah, mode = state.displayMode)
        }
    }
}

@Composable
private fun AyahBlock(ayah: AyahEntity, mode: DisplayMode) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Arabic + ornate marker — RTL 22sp Naskh (PHASE_6 spec, Rule 13)
        RtlText(
            text = "${ayah.arabicText} ${QuranText.arabicMarker(ayah.ayahNumber)}",
            fontSize = 22.sp,
            lineHeight = 38.sp,
            color = Color.White
        )
        when (mode) {
            DisplayMode.ARABIC_ONLY -> Unit
            DisplayMode.ARABIC_ENGLISH -> Text(
                text = QuranText.stripFootnoteMarkers(ayah.translationEn),
                fontFamily = com.mawaqit.app.ui.theme.InterFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
            DisplayMode.ARABIC_URDU -> RtlText(
                text = QuranText.stripFootnoteMarkers(ayah.translationUr),
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Rule 13: Arabic/Urdu text renders inside an RTL composition locally. */
@Composable
private fun RtlText(
    text: String,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = text,
            fontFamily = NotoNaskhArabicFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            style = TextStyle(textAlign = TextAlign.End),
            color = color,
            modifier = modifier.fillMaxWidth()
        )
    }
}

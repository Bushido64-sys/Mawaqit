package com.mawaqit.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mawaqit.app.MainActivity
import com.mawaqit.app.R
import com.mawaqit.app.data.model.NextPrayer
import com.mawaqit.app.data.repository.AyahRepository
import com.mawaqit.app.data.repository.PrayerRepository
import com.mawaqit.app.ui.components.prayerNameRes
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SurfaceDeep
import com.mawaqit.app.util.formatCountdown
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// PHASE-5.1: the three responsive layout anchors (SizeMode.Responsive — the
// system picks the best-fitting pre-rendered layout; smooth resize on
// Android 12+, "largest that fits" fallback pre-12). Sizes follow the
// developer.android.com build-ui pattern (minimum cell ≈ 70dp).
private val WIDGET_COMPACT = DpSize(180.dp, 60.dp)   // 2x1: no ayah line
private val WIDGET_REGULAR = DpSize(270.dp, 60.dp)   // 3x1: + ayah line
private val WIDGET_LARGE = DpSize(270.dp, 125.dp)    // 3x2+: hero fonts + city

/**
 * PHASE_5: the home screen widget (PHASE_5_WIDGET.md; colors from DESIGN.md
 * §1 — SurfaceDeep bg, PrimaryGold countdown, white text, same tokens as the
 * app's hero card).
 *
 * PHASE-5.1 (user feedback: content didn't reframe when resized): three
 * responsive layouts — 2x1 compact (prayer + time only), 3x1 regular (+ ayah
 * line), 3x2+ large (hero fonts, city label, 3-line ayah). Fonts scale with
 * the chosen layout, so bigger widget = bigger text, never stretched-small.
 *
 * Reads the next prayer + daily ayah DIRECTLY from the repositories at render
 * time (patched doc: no DataStore round-trip — Room is the single source of
 * truth, offline-first). Tap → opens the app. Redrawn by three independent
 * nets: the 15-min WorkManager heartbeat, a one-shot at the exact moment the
 * shown prayer passes, and the system's own 30-min updatePeriodMillis.
 */
class MawaqitWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(WIDGET_COMPACT, WIDGET_REGULAR, WIDGET_LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        // provideGlance runs on the main thread — DB reads move to IO (official docs).
        val repo = entry.prayerRepository()
        val next = withContext(Dispatchers.IO) {
            repo.getTodayPrayerTimes()?.let { repo.getNextPrayer(it) }
        }
        val ayah = withContext(Dispatchers.IO) { entry.ayahRepository().getDailyAyah() }
        val city = withContext(Dispatchers.IO) { repo.cityName().first() }

        // Resolve all strings OUTSIDE composition — RemoteViews contexts have
        // no stringResource; Context.getString is the safe path.
        val nextPrayerLabel = context.getString(R.string.next_prayer)
        val prayerName = next?.let { context.getString(prayerNameRes(it.name)) }.orEmpty()
        val countdownLine = next?.let {
            context.getString(R.string.widget_in, formatCountdown(it.timeMillis - System.currentTimeMillis()))
        }.orEmpty()
        val noDataText = context.getString(R.string.widget_no_data)
        val ayahLine = "${ayah.reference} · ${ayah.arabic}"
        // Pre-built outside composition (glance 1.1.0 has no reified
        // actionStartActivity<T>() — the explicit Intent overload is the
        // version-safe call; see build-fix 2026-09-19).
        val openAppIntent = Intent(context, MainActivity::class.java)

        provideContent {
            MawaqitWidgetContent(
                next = next,
                nextPrayerLabel = nextPrayerLabel,
                prayerName = prayerName,
                countdownLine = countdownLine,
                noDataText = noDataText,
                ayahLine = ayahLine,
                city = city.orEmpty(),
                openAppIntent = openAppIntent
            )
        }
    }
}

@Composable
private fun MawaqitWidgetContent(
    next: NextPrayer?,
    nextPrayerLabel: String,
    prayerName: String,
    countdownLine: String,
    noDataText: String,
    ayahLine: String,
    city: String,
    openAppIntent: Intent
) {
    val size = LocalSize.current
    val isWide = size.width >= WIDGET_REGULAR.width   // room for the ayah line
    val isLarge = size.height >= WIDGET_LARGE.height  // hero treatment

    // Fonts scale WITH the layout — bigger widget = bigger text (PHASE-5.1).
    val nameSize = if (isLarge) 26.sp else if (isWide) 20.sp else 18.sp
    val timeSize = if (isLarge) 20.sp else if (isWide) 16.sp else 14.sp
    val labelSize = if (isLarge) 12.sp else 11.sp
    val smallSize = if (isLarge) 12.sp else 11.sp

    val white = ColorProvider(Color.White)
    val whiteDim = ColorProvider(Color.White.copy(alpha = 0.7f))
    val gold = ColorProvider(PrimaryGold)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(SurfaceDeep))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(openAppIntent))
            .padding(
                horizontal = if (isLarge) 16.dp else 14.dp,
                vertical = if (isLarge) 12.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (next == null) {
            Text(
                text = noDataText,
                style = TextStyle(color = white, fontSize = 13.sp)
            )
        } else {
            if (isLarge) {
                // Header row: "Next Prayer" left, city right (large only).
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = nextPrayerLabel,
                        style = TextStyle(color = whiteDim, fontSize = labelSize)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    if (city.isNotBlank()) {
                        Text(
                            text = city.uppercase(),
                            style = TextStyle(color = whiteDim, fontSize = labelSize)
                        )
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    if (!isLarge) {
                        Text(
                            text = nextPrayerLabel,
                            style = TextStyle(color = whiteDim, fontSize = labelSize)
                        )
                    }
                    Text(
                        text = prayerName,
                        style = TextStyle(color = white, fontSize = nameSize, fontWeight = FontWeight.Bold)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = next.timeStr,
                        style = TextStyle(color = gold, fontSize = timeSize, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = countdownLine,
                        style = TextStyle(color = gold, fontSize = smallSize)
                    )
                }
            }

            if (isLarge) {
                Spacer(GlanceModifier.defaultWeight()) // push ayah to the bottom
            } else {
                Spacer(GlanceModifier.height(4.dp))
            }
            if (isWide) {
                Text(
                    text = ayahLine,
                    maxLines = if (isLarge) 3 else 1,
                    style = TextStyle(color = whiteDim, fontSize = smallSize)
                )
            }
        }
    }
}

/** Manifest-registered receiver (APPWIDGET_UPDATE + provider meta-data). */
class MawaqitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MawaqitWidget()
}

/** Hilt bridge: the widget is instantiated by the OS, not by Hilt. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun prayerRepository(): PrayerRepository
    fun ayahRepository(): AyahRepository
}

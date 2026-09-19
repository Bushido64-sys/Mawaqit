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

// PHASE-5.2: anchors matched to REAL launcher grid slots (~70dp per cell with
// margins). The 5.1 anchors were too wide (270dp > a real 3x2's ~210–240dp),
// so the system ALWAYS fell back to compact — that's why the fonts never
// grew. Real sizes → each layout actually gets selected in its slot.
private val WIDGET_COMPACT = DpSize(130.dp, 60.dp)   // 2x1
private val WIDGET_REGULAR = DpSize(195.dp, 60.dp)   // 3x1
private val WIDGET_TALL    = DpSize(130.dp, 170.dp)  // 2x2: stacked, big name
private val WIDGET_LARGE   = DpSize(195.dp, 170.dp)  // 3x2+: hero treatment

/**
 * PHASE_5: the home screen widget (PHASE_5_WIDGET.md; colors from DESIGN.md
 * §1 — SurfaceDeep bg, PrimaryGold countdown, white text, same tokens as the
 * app's hero card).
 *
 * PHASE-5.2: four responsive layouts with fonts that REALLY scale with size
 * (user request: "way bigger") — 2x1 compact row, 3x1 regular row (+ayah),
 * 2x2 tall stack (name on its own line), 3x2+ hero (28sp name, city label,
 * 3-line ayah). Pattern per official docs: branch on LocalSize inside
 * provideContent; system picks the best-fitting pre-rendered layout.
 *
 * Reads the next prayer + daily ayah DIRECTLY from the repositories at render
 * time (offline-first). Tap → opens the app. Refreshed by three independent
 * nets: the 15-min WorkManager heartbeat, a one-shot at the exact moment the
 * shown prayer passes, and the system's own 30-min updatePeriodMillis.
 */
class MawaqitWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(WIDGET_COMPACT, WIDGET_REGULAR, WIDGET_TALL, WIDGET_LARGE)
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
    val isWide = size.width >= WIDGET_REGULAR.width    // 3x1 / 3x2
    val isTall = size.height >= WIDGET_TALL.height     // 2x2 / 3x2
    val isHero = isWide && isTall                      // 3x2+

    // Font scale per layout — PHASE-5.2 "way bigger" targets:
    // name 16/20/24/28sp · time 13/16/18/22sp · left 10/11/13/15sp
    val nameSize = when {
        isHero -> 28.sp
        isTall -> 24.sp
        isWide -> 20.sp
        else -> 16.sp
    }
    val timeSize = when {
        isHero -> 22.sp
        isTall -> 18.sp
        isWide -> 16.sp
        else -> 13.sp
    }
    val countdownSize = when {
        isHero -> 15.sp
        isTall -> 13.sp
        isWide -> 11.sp
        else -> 10.sp
    }
    val labelSize = when {
        isHero -> 12.sp
        isTall -> 11.sp
        isWide -> 10.sp
        else -> 9.sp
    }
    val ayahSize = if (isHero) 15.sp else if (isTall) 13.sp else 11.sp
    val ayahMaxLines = when {
        isHero -> 3
        isTall -> 2
        isWide -> 1
        else -> 0 // no room in 2x1 — hidden by design (was the cut-off bug)
    }

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
                horizontal = if (isTall) 16.dp else 14.dp,
                vertical = if (isTall) 14.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (next == null) {
            Text(
                text = noDataText,
                style = TextStyle(color = white, fontSize = 13.sp)
            )
        } else if (isTall) {
            // ── 2x2 tall stack / 3x2 hero: name owns its own line ──
            if (isHero) {
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
            } else {
                Text(
                    text = nextPrayerLabel,
                    style = TextStyle(color = whiteDim, fontSize = labelSize)
                )
            }
            Spacer(GlanceModifier.height(if (isHero) 8.dp else 4.dp))
            Text(
                text = prayerName,
                style = TextStyle(color = white, fontSize = nameSize, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = next.timeStr,
                style = TextStyle(color = gold, fontSize = timeSize, fontWeight = FontWeight.Bold)
            )
            Text(
                text = countdownLine,
                style = TextStyle(color = gold, fontSize = countdownSize)
            )
            Spacer(GlanceModifier.defaultWeight()) // push the ayah to the bottom
            if (ayahMaxLines > 0) {
                Text(
                    text = ayahLine,
                    maxLines = ayahMaxLines,
                    style = TextStyle(color = whiteDim, fontSize = ayahSize)
                )
            }
        } else {
            // ── 2x1 compact / 3x1 regular: side-by-side row ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = nextPrayerLabel,
                        style = TextStyle(color = whiteDim, fontSize = labelSize)
                    )
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
                        style = TextStyle(color = gold, fontSize = countdownSize)
                    )
                }
            }
            if (ayahMaxLines > 0) {
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = ayahLine,
                    maxLines = ayahMaxLines,
                    style = TextStyle(color = whiteDim, fontSize = ayahSize)
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

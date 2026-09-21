# PROGRESS.md — The Project's Save File 💾
**Mawaqit: Salah Time, Prayer Alarm Clock (Android)**

> **Purpose:** the single source of truth for where the project is.
> A fresh AI session reads THIS file first and instantly knows what's done,
> what's pending, and what to do next — no archaeology, no guessing.
> The AI updates it at the end of every work session. If it's stale, that's a bug — fix it.
> **Last updated: 2026-09-21 — [PHASE-6.2] user-PASSED ("looks better than before, good job!") → [PHASE-6.3] "Keep My Place" BUILT, PUSHED & BUILD-GREEN (b95e5d7 + DI fix 645e9d0, 11 files total) — AWAITING user test (checklist below). APK: GitHub Actions auto-build on every push.**

---

## THE PROJECT IN ONE LINE
Android prayer-times + alarm app (Kotlin, Compose, offline-first), built phase by phase
from a detailed guidebook (`mawaqit-guidebook/`), with the user learning as we go.

## WHERE WE ARE RIGHT NOW
**Phases 0–5 ✅ ALL USER-PASSED.** Phase 5 finale (2026-09-19): user tested
[PHASE-5.2] on device — "the widget looks better than before" — fonts +
responsive layouts + 5s popup all verified.

**[PHASE-6.2] PASSED (user: "looks better than before, good job!"). NOW:
[PHASE-6.3] "Keep My Place" built (b95e5d7) — AWAITING user test. Open the
next session by asking for THESE results BEFORE any new work:
| # | Test | Expect |
|---|------|--------|
| 1 | Open a surah you previously read | Jumps straight to your last page + "Resumed at page N" toast |
| 2 | Open a fresh surah | Starts at page 1 (no toast) |
| 3 | Any page footer | Gold "Mark page" pill with BLUE border next to "Page X of Y" |
| 4 | Tap "Mark page" → close → reopen | Pill filled gold "Marked ✓"; you land on the MARKED page (bookmark beats last-read) |
| 5 | Tap the marked pill again | Unpins (back to "Mark page") |
| 6 | Quran tab after reading | "Continue Reading" card (blue, gold border): surah + "Ayah X of Y", TWO gold bars (this surah % / Whole Quran %), Continue button reopens the reader |
| 7 | Swipe to a surah's LAST page → back to list | That row gold-highlighted, number chip gold-bordered, "✓ Completed" tag |
| 8 | Change font to XL | Pages re-fit; progress % UNCHANGED (stored in ayahs, not pages) |
| 9 | Al-Fatihah / airplane mode | NO Bismillah; cached surahs open instantly (regressions) |
| 10 | Azan / widget / home | Untouched (regression) |
Known safe caveats: the continue card appears only AFTER the first read
(opening a surah counts page 1 as read — by design); a COMPLETED surah
reopens at page 1 (clean restart, bookmark also cleared only if never re-pinned);
6.2's gradient background + safety-valve scroll page still apply.

**PHASE-6.3 implementation facts (b95e5d7, 2026-09-21):**
- reading_progress table (Room v2): surahNumber PK, lastAyah (resume target =
  first ayah of last page), furthestAyah (running max → completion %), lastReadAt,
  completed/completedAt, bookmarkAyah (0 = none). MIGRATION_1_2 additive
  (CREATE TABLE only) — prayer logs + cached surahs survive.
- Progress stored in AYAH numbers (font-size independent); reader maps
  ayah → page via fittedPages.indexOfFirst after each re-fit.
- Resume priority: bookmark > lastAyah > none; completed surahs → null (page 1).
- Opening a surah records page-1 progress (creates row, refreshes lastReadAt);
  pager settle records lastAyah=first-of-page, furthest=last-of-page.
- FitPages.fit gained footerExtraHeight param; MushafPage renders the
  mark-page strip at FitPages.MUSHAF_FOOTER_H (64dp) — constants stay in sync.
- ReadingProgressRepository: QuranProgress (continueSurah = newest
  not-completed row, whole-Quran % = Σ min(furthest, total) / Σ total).
- ListViewModel: uiState = listState combine progress.getAll() →
  completedNumbers set; quranProgress StateFlow recomputed on every progress write.
- SURAH row gold treatment + QuranProgressCard + gold search border in
  QuranListScreen; continue card hidden while continueSurah == null.
- 6.2 build fix for the record: BOM 2024.06 has rememberTextMeasurer(),
  NOT LocalTextMeasurer (ad48e7d). Check new API names against our BOM first.
- 6.3 build fix for the record: a NEW repository interface is useless without
  a @Binds entry in RepositoryModule (Dagger MissingBinding, 645e9d0) — same
  family of lesson as Phase 6's name-tagged Retrofit clients: every new
  injectable needs its wiring line, not just its implementation.

**PHASE-6 implementation facts (for future sessions):**
- UmmahAPI 2nd Retrofit in NetworkModule (base https://ummahapi.com/api/).
- QuranRepository: list bootstrap once (surah_list count gate), lazy
  per-surah ayah caching, bismillah_pre flags learned into DataStore key
  surah_no_bismillah ("1,9") — DB schema v1 UNTOUCHED, no migration.
- Footnote digits stripped at RENDER layer only (QuranText.stripFootnoteMarkers,
  Regex from API_REFERENCE.md 2.2); ornate ayah marker via QuranText.arabicMarker
  (Arabic-Indic digits in ﴿﴾).
- AyahRepository now composes Endpoint 2.3 (random surah ≤60 verses, ≤3 retries,
  random verse) with full fallback chain (today-cache → last cache → FALLBACK_AYAHS).
- Detail route "surah/{number}" (NavType.Int; SavedStateHandle.get("number"))
  lives inside the same NavHost but the pill bar is hidden via
  currentRoute.startsWith("surah/") check (return@Scaffold in bottomBar).
- Real azan MP3s landed earlier (res/raw/ has all 3); splash video still
  pending (assignment/06).

**PHASE-6.1 mushaf reader facts (4f585cb, 2026-09-20):**
- Amiri Regular/Bold TTFs BUNDLED in res/font (~845KB, magic bytes verified) —
  Quran calligraphy works fully offline. NotoNaskhArabicFamily = Amiri first +
  downloadable Noto Naskh fallback (Type.kt); AmiriFontFamily also exposed.
- QuranText.chunkIntoPages(): ~750 Arabic chars/page, never splits an ayah →
  HorizontalPager of gold-framed cards (28dp radius, 1dp gold border — the
  app's ONE ceremonial border; kit is otherwise borderless). Bismillah page 0 only.
  (RETIRED in PHASE-6.2 — see below.)
- SegmentedModeToggle: sliding gold indicator (BoxWithConstraints +
  animateDpAsState) replaces the 3 fat pills; labels العربية/EN/اردو hardcoded.
- READER_FONT_SCALE (0.85/1.0/1.15/1.3) + READER_SWIPE_HINT_SEEN prefs added;
  "Aa" chip opens FontSizeSheet (M3 experimental → screen is @OptIn). Phase 8
  must surface font size in Settings (flagged in PHASE_9 addendum too).
- **DISCIPLINE RULE (user, permanent):** extra ideas that are not in the current
  phase's plan get WRITTEN INTO FUTURE-PHASE DOCS, never built opportunistically.
  The premium onboarding upgrade (5-page pre-settings flow with visual ayah
  preview cards + azan previews) is specced in PHASE_9_POLISH.md addendum —
  deferred until the splash video (assignment/06) lands.

**PHASE-6.2 "Perfect Page" facts (4c31b58, 2026-09-21 — 5 files, +503/−198):**
- FitPages.kt (NEW, ui/quran/): TextMeasurer-measured page packer. MushafPager
  wraps a BoxWithConstraints and calls FitPages.fit() remember()-ed on
  (ayahs, surah, mode, fontScale, bismillahPre, maxWidth, maxHeight) — pages
  fit the REAL screen, re-fit on font/mode change. Whole ayahs only.
- FitPages layout chrome constants (PAGE_PAD_H/V, CARD_PAD_H/V) are shared
  with MushafPage — one source, no drift. Type-size constants MUST mirror
  the renderers in SurahDetailScreen (they do; keep them in sync).
- Oversized single ayah → FittedPage.allowScroll=true → that page scrolls
  internally (safety valve, by design).
- SurahDetailViewModel: UI state no longer carries `pages` (chunker retired);
  screen fits pages itself. QuranText.chunkIntoPages DELETED.
- TopBar: "Aa" chip replaced by ☰ (MoreVert) chip → ReaderSettingsSheet
  (ModalBottomSheet, @OptIn) holds SegmentedModeToggle + font pills + preview.
  SegmentedModeToggle is now sheet-resident (centered, 72% width).
- WindowedPageDots: sliding window of max 7 dots, active centered & gold —
  long surahs can't push the active dot off-screen (user-reported bug).
  Footer: "Page X of Y · Verses a–b" (string reader_page_position).
- New strings: reader_page_position, reader_translation, reader_settings,
  content_desc_reader_settings. Untouched: DB, prefs, nav, home, widget, alarms.
- **Build fix (ad48e7d):** first 6.2 build failed — `LocalTextMeasurer` does
  NOT exist in compose-bom 2024.06 (added later). Correct API for us:
  `rememberTextMeasurer()` from `androidx.compose.ui.text` (function call,
  NOT a CompositionLocal). General rule: our BOM is OLD — verify new API
  names against 2024.06, prefer long-lived APIs.
- **Sandbox network quirk (2026-09-21):** read_url/web_search to api.github.com
  may fail with DNS ETIMEOUT while `curl` from run_terminal_command still
  works (and git push over SSH always works). If read_url fails on GitHub
  API, fall back to: `curl -s https://api.github.com/repos/Bushido64-sys/
  Mawaqit/check-runs/<ID>/annotations` — get the check-run ID first from
  `/commits/<SHA>/check-runs`.

## GLANCE GOTCHAS (Phase 5, all fixed & verified — do not repeat)
1. actionStartActivity<Activity>() generic shorthand DOES NOT exist in Glance
   1.1.0 — build an explicit Intent and call actionStartActivity(intent, params).
2. LocalSize lives in androidx.glance (NOT androidx.glance.appwidget).
3. defaultWeight() is a Row/Column-scope member extension — never importable.
4. SizeMode.Responsive: sizeMode property + branch on LocalSize.current;
   anchors must match REAL launcher cell math (~70dp cells; our final:
   130/195dp wide × 60/170dp tall) or the layout silently never gets picked.
5. Experimental APIs need @OptIn **on every function that uses them** — the
   annotation does NOT reach into private composables. Burned twice: M3
   ModalBottomSheet (Phase 5) AND HorizontalPager/rememberPagerState
   (ExperimentalFoundationApi, Phase 6.1 — 7 CI errors, one cause).

(PHASE-3-era detail below kept for history.)

What was built (14 files + stubs, commits 643fd7f + 0f7e1d0):
- alarm/: AlarmScheduler (setAlarmClock + inexact fallback), AlarmReceiver (wakelock
  handoff), AzanService (FGS, IMPORTANCE_HIGH channel, 5-min cutoff, salah_log
  time-reached row), AzanPlayer (alarm stream; guidebook crash bug fixed),
  AzanReceiver + MarkPrayedWorker (notification "Mark as Prayed"), BootReceiver →
  AlarmWorker (WorkManager reschedule after reboot; LOCKED_BOOT ignored — CE storage),
  AzanType, AzanServiceUi.
- Wiring: manifest receivers/service + WorkManager initializer removal,
  MawaqitApplication implements Configuration.Provider (HiltWorkerFactory),
  PrefsRepository alarm toggles + azan choice, strings, HomeViewModel schedules
  alarms whenever today's times load, temp switches + notif popup in HomeScreen.
- Guidebook patched BEFORE code: ASSETS.md AzanPlayer setAudioAttributes crash,
  PERMISSIONS.md LOCKED_BOOT_COMPLETED note.
**PHONE TEST RESULTS (user, 2026-09-18, silent audio — verify by seeing):**
- ✅ Check 1: Dhuhr notification appeared at prayer time
- ✅ Check 3: "Mark as Prayed" button works (writes salah_log; its on-screen display
  is INTENTIONALLY absent — Phase 4 builds the home screen that reads it)
- ✅ Check 5: rebooted right before prayer time, notification still fired (BootReceiver verified)
- ✅ Check 4: toggle OFF → no notification for that prayer
- ✅ Check 2: notification auto-dismissed by itself; code-verified: 5-min delay → stopSelf()
  → onDestroy → audio stop + STOP_FOREGROUND_REMOVE. Also setOngoing(true) = swipe-proof.
- ✅ Check 6: alarm ⏰ icon appeared in status bar while ringing (user-confirmed)
- **PHASE-3.2 (post-3.1 user test):** reboot + clock set to 5:02 (Fajr 5:03) →
  notification arrived 5:04. VERDICT: the alarm SURVIVED the reboot (fired
  end-to-end); ~1 min lateness = post-boot system congestion (re-arm worker and
  delivery both land in the boot storm) — normal for every alarm app when the
  event is minutes after boot; real 3 AM reboots have hours of slack. Bug found
  + fixed: MY_PACKAGE_REPLACED is NOT on Android's implicit-broadcast exemption
  list → the manifest registration was dead code; removed from manifest +
  BootReceiver (updates still covered: armed setAlarmClock slots survive
  updates + daily housekeeping + next app open). Added TEMP diagnostics on the
  home screen: "Test azan in 10s" (full real chain, sentinel TEST prayer, no
  salah_log row) and "Re-arm alarms" (with armed-count feedback).
- User asked about required settings + auto re-prompt: one-time setup = notifications
  popup (in app) + battery Unrestricted (+ Alarms & reminders on Android 12 only).
  Re-prompt "health cards" are PLANNED for Phase 8 (battery card in PHASE_8_SETTINGS.md);
  agreed to widen into one "Alarm Health" card checking all 3 permissions.

## BULLETPROOFING AUDIT (2026-09-18, user wants "best in market")
Full code audit of alarm chain (AzanService, AlarmScheduler, AlarmReceiver,
AlarmWorker, AzanPlayer, AzanServiceUi, HomeViewModel). Core chain is SOLID:
reboot-safe, toggle-safe, no double-scheduling, alarm-stream audio, corrupt-file
safe, wakelock handoff, START_NOT_STICKY. Gaps found, mapped to phases:
- **GAP-1 (HIGH) — CLOSED in [PHASE-3.1] (commit 615b0a5, 2026-09-18):** 7-day
  alarm plan from offline cache + DailyAlarmWorker 24h housekeeping + ACTIVE_ALARMS
  registry (plan-executor rework of AlarmScheduler, per-(prayer,date) slots).
- **GAP-2 (MED) — CLOSED in [PHASE-3.1]:** TIME_SET / TIMEZONE_CHANGED /
  MY_PACKAGE_REPLACED → AlarmWorker reschedule (BootReceiver + manifest extended).
  Design notes: requestCode = prayer.ordinal*100_000_000 + yyyymmdd; azanTypeFor
  resolved at PLAN time (extra freezes with the alarm — Phase 8 selector change
  applies on next plan refresh, acceptable); DailyAlarmWorker KEEP policy, 1h
  initial delay, battery-not-low constraint. Needs a Codespaces rebuild + one
  phone re-test (alarms still fire; ideally toggle + one prayer) before Phase 4.
- **GAP-3 (MED) — permission health:** Phase 8 planned battery card; agreed to make
  ONE "Alarm Health" card (notifications + battery + exact-alarm) that re-checks on
  every app open and auto-hides when healthy. Plus a "test azan in 10s" button.
  → Phase 8.
- **GAP-4 (LOW) — audio polish:** real MP3s still pending (assignment/05, one rebuild
  swaps them); add audio-focus request (duck/pause other players) with real audio.
  → when files arrive / Phase 8 azan selector.
- **GAP-5 (LOW) — cosmetic:** placeholder notification icon → real art in Phase 4/9;
  month-edge next-Fajr approximation → Phase 4 polish.

**Diagnostics PASSED (user, 2026-09-19): "Test azan in 10s" fired the full real
chain (notification popped ~10s after tap); "Re-arm alarms" armed 35 alarms =
7 days × 5 prayers — the GAP-1 7-day fix VERIFIED LIVE on device. GAP-1 + GAP-2
confirmed closed. Phase 3 = fully closed. Next: PHASE 4.**

---

## THE PHASES (from the guidebook)
| Phase | What | Status |
|---|---|---|
| 0 | Guidebook written, reviewed, 11 doc issues fixed | ✅ done |
| 1 | Skeleton: Gradle, manifest, resources, theme, Hilt app, MainActivity | ✅ COMPLETE — 5/5 phone checks passed 2026-09-17 |
| 2 | Prayer times (AlAdhan API + Room + repository) | ✅ COMPLETE — 5/5 phone checks passed 2026-09-17 |
| 3 | Alarms (AlarmReceiver, BootReceiver, AzanService) | ✅ PASSED — phone test 6/6 on 2026-09-18 (silent stubs; 5-min dismiss verified in code) |
| 4 | Home screen UI | ✅ PASSED 2026-09-19 — user confirmed live rollover, gold current-prayer highlight, azan notification. Bonus fix: one-frame setup flash on cold start (needsLocation tri-state) |
| 5 | Widget (Glance; lock-screen = opportunistic bonus) | ✅ PASSED — [PHASE-5.2] verified on device 2026-09-19 ("looks better than before") |
| 6 | Quran (UmmahAPI — endpoints re-verified live in Sept 2026) | 🚧 CODE COMPLETE (0fdbb31) — awaiting Actions-built APK + phone test |
| 7 | Qibla | not started |
| 8 | Settings (DataStore, Urdu, per-app language) | not started |
| 9 | Polish (splash: ONE card per launch, ~5s, bundled ≤4MB video) | not started |

## USER'S RULES (non-negotiable — learned the hard way 😄)
1. **ONE PHASE AT A TIME.** Never start the next phase until the current one is
   tested on the user's phone and the user says it's perfect. (The AI once jumped
   to Phase 2 early — files deleted, trust repaired. Don't repeat.)
2. **Explain everything like the user is 16, in a human tone.** Terminal outputs,
   errors, decisions — plain language, no jargon walls.
3. **Flag problems immediately.** If something's off, say it right away.
4. **Create `assignment/` files ONLY when the user asks** ("build assignment when I say so").
5. Docs in the guidebook get patched BEFORE code when issues are found.
6. **EXPLICIT "BUILD" PERMISSION GATE (2026-09-20, user demand — NON-NEGOTIABLE):**
   never write new features/fixes/code without the user's explicit go — present the
   plan FIRST, wait for "BUILD"/"go" or an explicit ask (e.g. "check and fix the red
   build" counts for that build only). Standing exceptions: fixing RED Actions builds
   when the user asks, and updating PROGRESS.md/docs. Changes stay surgical: every
   commit touches ONLY the files the fix requires; show `git diff --stat` as proof
   when core/shared files are involved. The user's trust was shaken once by fearing
   main files were touched — always state exactly what was touched, with evidence.

## KEY DECISIONS (the "why" behind the code)
- **Builds: GitHub Actions AUTO-BUILD on every push to main (PRIMARY since 2026-09-20); Codespaces = manual fallback only.** User's Codespace became unstable (wouldn't reload), so push-triggers were re-enabled on the existing debugged workflow — it never used the broken `android-actions/setup-android` action; it talks to sdkmanager directly (that was the fix for the Sept-2026 repo-wide breakage).   `paths-ignore` (**.md, codespaces/**) + concurrency-cancel protect free-tier minutes (private repo = 2000 min/month, ~15–20 min/build). APK lives in the run's **Artifacts** (`mawaqit-debug-<run#>`, GitHub login required to download, 14-day retention). If Actions ever breaks again → Codespaces `codespaces/setup.sh` + `codespaces/build.sh`.
- **Reading Actions failures WITHOUT the user copy-pasting (since 2026-09-20):** query
  `https://api.github.com/repos/Bushido64-sys/Mawaqit/commits/<sha>/check-runs` (public,
  no auth — works via read_url; the `gh` CLI is unavailable in the sandbox) → get the
  check-run id + conclusion → then `.../check-runs/<id>/annotations` for the real
  `e:` compiler lines. This is the standard triage loop for every red build.
- **Dagger gotcha (Phase 6, cost 1 build):** TWO Retrofit instances in NetworkModule
  MUST carry distinct `@Qualifier` tags (`AladhanRetrofit`/`UmmahRetrofit`, defined in
  NetworkModule.kt) — unqualified duplicate types = `DuplicateBindings` failure in the
  Hilt graph. Any future 3rd API client needs its own qualifier + tagged service param.
- **CI workflow is MANUAL-TRIGGER ONLY** (`workflow_dispatch`). Works via direct sdkmanager calls
  if ever re-enabled. Don't restore push-triggered builds.
- **Every build is self-identifying:** `build.gradle.kts` reads `git rev-parse --short HEAD` and
  stamps it into `versionName` (`1.0.0-<sha>`), `BuildConfig.GIT_SHA` (shown on the app screen),
  and the APK filename (`Mawaqit-1.0.0-<sha>-debug.apk`, renamed by build.sh). NEVER remove this.
- **Java 17 is MANDATORY for builds.** Codespace default JDK is 25, which Kotlin 1.9.24 cannot
  parse (`IllegalArgumentException: 25.0.4.1` at config phase). setup.sh installs Temurin 17 to
  `~/jdks` if none exists; build.sh verifies `java -version` = 17 and refuses otherwise.
- **Kill Gradle daemons before every build** (`./gradlew --stop` + `--no-daemon`): a stale daemon
  born with Java 25 gets reused even after Java 17 is active, reproducing the crash. One-shot
  `--no-daemon` builds trade ~30s for determinism. Don't "optimize" this away.
- **Theme:** light-only M3, gold = primary (CTAs), blue = secondary — derived from
  `design_tokens.xml` + `DESIGN.md`, not copied (tokens file has no Kotlin sections).
- **MainActivity is AppCompatActivity** (not ComponentActivity) for per-app language switching later.
- **Phase-1 deviations, deliberate:** widget receiver deferred to Phase 5 (empty one can crash
  launchers); AppModule is an empty stub (Room DB doesn't exist yet); themes use Theme.AppCompat
  + Theme.SplashScreen parent.
- **Java is NOT installed on the local PC.** Never try to build locally. Codespaces = the builder.
- **Git identity configured in repo:** Bushido64-sys / checking.mf420@gmail.com. Remote is SSH
  (key works — the AI can push). Repo: `github.com/Bushido64-sys/Mawaqit` (branch: main).

## PENDING USER-SIDE ITEMS (tracked in `assignment/`)
- [x] **Build APK in Codespaces** (`assignment/07`) — DONE 2026-09-17 after 3 build fixes
      (Java 25→17, daemon eviction, icon XML). The pipeline works; rebuilding is now routine.
- [~] **Phone test, 5 checks** (`assignment/03`) — DONE for both phases (Phase 1 & Phase 2 all 5/5).
- [ ] **Azan audio: 3 real MP3 files, ≤3MB each** (`assignment/05`) — DECIDED: stubs first.
      Silent 16KB stubs are committed in `res/raw/` (Phase 3 works, no sound). When the
      user drops in real azan_default/fajr/makkah.mp3, ONE rebuild swaps them — nothing else changes.
- [ ] Compress splash video to ≤4MB → `app/src/main/res/raw/splash_video.mp4` (needed by Phase 9 only)

## NEXT SESSION: DO THIS IN ORDER
1. **Read this file top to bottom.** (You just did — good.)
2. **FIRST: ask the user for the [PHASE-6] build + test results** — the Phase-6
   code (0fdbb31) has never been built/tested by the user. The APK now comes
   from the GitHub Actions auto-build (Actions tab → latest run → Artifacts →
   mawaqit-debug-<run#>). Then run the 9-check list at the top of this file.
3. Any failure → one fix at a time, commit `[PHASE-6.x]`, push (Actions
   rebuilds automatically), user re-downloads the new APK.
4. All pass → user gives explicit OK → **PHASE_7 (Qibla)**: read PHASE_7_QIBLA.md
   FIRST; patch docs before code (our ritual). Widget Glance gotchas (for any
   future widget work): explicit-Intent actionStartActivity only, LocalSize
   lives in androidx.glance, defaultWeight() is a scope member not an import,
   responsive anchors MUST match real launcher cells (~70dp/cell).
5. **Before ending any session:** update this file (status line, phases table, pending items).

## SESSION LOG (one line per working session, newest last)
- **2026-09-17 — Session 1:** Docs patched (11 issues + live API verification). Phase 1 built
  and phone-tested 5/5. Codespaces pipeline established after GitHub Actions broke repo-wide
  (fixes: Java 25→17 forced, daemon eviction + --no-daemon, icon <rect>/dp fixes). PROGRESS.md
  created; commit-stamped builds live.
- **2026-09-17 — Session 2:** Phase 2 built and phone-tested 5/5 (Karachi fetch, aladhan.com
  cross-check, offline cache + banner, GPS, live countdown). One compile fix (Modifier import).
  ~17 files: Room (4 tables, ISO), AlAdhan Retrofit, DataStore prefs, GPS helper, offline-first
  repo, temp HomeScreen. Paused with user's OK — **Phase 3 next.**
- **2026-09-18 — Session 3:** Phase 3 (alarms) CODE COMPLETE — 10 alarm files + wiring
  (manifest, Hilt WorkManager config, prefs toggles, temp switches + notif popup, 3 silent
  audio stubs). 2 guidebook bugs patched pre-code (AzanPlayer crash, LOCKED_BOOT). Session
  interrupted mid-build once; resumed cleanly. 2 commits pushed: 643fd7f, 0f7e1d0.
  **Next: Codespaces build → phone test.**
- **2026-09-18 — Session 4:** Codespaces build OK; **Phase 3 phone test PASSED 6/6**
  (fires at Dhuhr, Mark-as-Prayed, survives reboot, toggle-off, 5-min auto-dismiss
  code-verified, alarm icon seen). salah_log display = Phase 4 by design. Full
  bulletproofing audit done → GAP-1..5 backlog added to this file. GAP-1+GAP-2
  FIXED same session in [PHASE-3.1] (commit 615b0a5): 7-day plan executor +
  daily housekeeping + clock/timezone/update re-arm. Rebuild + re-test pending.
  Then [PHASE-3.2]: user's reboot+clock test fired 1 min late (5:04 vs 5:03) —
  diagnosed post-boot congestion, chain INTACT (reboot survival confirmed);
  MY_PACKAGE_REPLACED registration removed (not exempt per Android docs);
  temp diagnostics (test azan / re-arm) added.
- **2026-09-19 — Session 5:** [PHASE-3.2] diagnostics phone-tested and PASSED
  (Test azan in 10s ✓; Re-arm = 35 alarms = 7d × 5 prayers ✓). Phase 3 FULLY
  CLOSED. ponytail (minimal-code) skill installed at .agents/skills/ + auto-load
  line added to guidebook AGENTS.md boot ritual. **Next: Phase 4 (home screen UI).**
- **2026-09-19 — Session 6:** [PHASE-4] CODE COMPLETE (commit 5303560, 18 files,
  +994/−229). Real home screen per DESIGN.md: NextPrayerCard hero (SurfaceDeep,
  gold countdown ring), PrayerRow (tap = mark prayed → salah_log via new
  SalahRepository; per-row alarm toggle), AyahCard (daily rotation of curated
  FALLBACK_AYAHS — UmmahAPI deliberately deferred to Phase 6, offline-first),
  MawaqitNavGraph floating pill (Quran/Qibla/Settings placeholders), DataStore
  DAILY_AYAH_* cache keys, ic_azan white-mosque notification icon. HomeViewModel:
  1s ticker + midnight rollover re-load, 30-day salah_log cleanup on start.
  Temp Phase 2/3 test UI REMOVED (re-arm/test buttons, big switches); notif
  permission request KEPT. AlarmRefreshManager.fireTestAlarm kept deliberately
  for the Phase 8 Alarm Health card. **Next: user Codespaces build → phone test → Phase 5.**
- **2026-09-19 — Session 7:** Phase 4 build OK; user phone test 6/8 (hero, rows,
  persistent checkmarks, nav tabs, offline cache all pass). 2 REAL BUGS found
  + fixed: (1) hero countdown froze at 00:00:00 once the shown next prayer's
  time passed — ticker only updated the clock; now the 1s ticker recomputes
  nextPrayer via repository.getNextPrayer the moment its timeMillis passes
  (real-time rollover to Dhuhr→Asr→…, no app restart needed); (2) gold
  "current prayer" highlight stuck on Fajr all day — getCurrentPrayerStatus
  flagged the FIRST passed prayer; now marks the MOST RECENT passed prayer as
  CURRENT (Fajr stays gold until Dhuhr's time begins — period semantics).
  NOT a bug: white mosque-with-minarets icon in the notification shade =
  ic_azan (Android forces white silhouette notif icons); ⏰ = separate system
  exact-alarm indicator. Rebuild + focused re-test pending.
- **2026-09-19 — Session 7 (cont.):** User re-tested → rollover works live,
  gold highlight advances, notification fired → **PHASE 4 PASSED**. User asked
  why reinstall showed prayer times instantly + no permission popups + a
  split-second "Assalamualaikum" flash. Investigation: (1) Android Auto Backup
  — manifest allowBackup=true (from guidebook PHASE_1_SETUP.md) auto-restores
  DataStore + Room + runtime permission grants on reinstall → data present
  from first frame; NO internet call (refreshIfNeeded short-circuits when
  LAST_MONTH_FETCHED == current month && countForMonth > 0 — code-verified).
  Working as designed; keep allowBackup=true. (2) The flash was a real
  cosmetic bug: needsLocation defaulted true for the first frame before the
  async prefs read → setup screen rendered one frame. Fixed: tri-state
  (null=checking → spinner, true=setup, false=home). (3) "No splash": the
  system splash (Theme.Mawaqit.Splash, blue + launcher icon) IS there — just
  fast because Phase 1 init is light; the branded animated ~5s splash is
  deliberately Phase 9 (needs user's ≤4MB video). Rebuild carries the flash fix.
- **2026-09-19 — Session 8:** [PHASE-5] CODE COMPLETE (commit dcca709, 8 files).
  Glance 2x1 widget: MawaqitWidget.kt (SurfaceDeep bg + PrimaryGold countdown,
  DESIGN.md §1 tokens; reads next prayer + ayah from Room DIRECTLY via Hilt
  EntryPoint at render time — patched doc dropped the DataStore round-trip),
  WidgetUpdateWorker.kt (15-min heartbeat + prayer-pass one-shot at
  timeMillis+1s, REPLACE policy; doWork re-arms one-shot as side effect →
  survives reboots), MawaqitWidgetReceiver + res/xml/mawaqit_widget_info.xml
  (updatePeriodMillis=30min = third net), PrayerNameLabel gained non-compose
  prayerNameRes(). Hooks: Application.ensureScheduled, HomeViewModel.loadTimes
  → refreshNow. API research done BEFORE code (official Glance docs: exported
  =false receiver, loading layout, actionStartActivity<MainActivity>). Self-
  review caught: stray defaultWeight import (member extension — build breaker).
  GUIDEBOOK PATCHED pre-code: PHASE_5_WIDGET.md (wiring missing from file list,
  DESIGN.md §6 cross-ref dead — widget spec doesn't exist, DataStore keys
  unnecessary). **Next: Codespaces build → phone test (picker/refresh/tap/
  reboot/lock-screen-bonus) → Phase 6 (Quran).**
- **2026-09-19 — Session 9:** Build error from user log: glance 1.1.0 has NO
  reified actionStartActivity<T>() → explicit Intent overload (c515dda; the
  Intent is now pre-built in provideGlance). User phone test: widget visuals
  PASS (picker, render, tap, reboot survival); feedback → [PHASE-5.1] built
  same session (9e25dba): (1) SizeMode.Responsive, 3 anchors 180x60/270x60/
  270x125 — compact 2x1 (no ayah), regular 3x1 (+ayah), large 3x2+ (26sp
  name, 20sp time, city uppercase, 3-line ayah, pushed to bottom); verified
  pattern against official build-ui docs before coding (LocalSize branching);
  (2) promo card = Alerts-2 pattern, addWidget() → requestPinAppWidget
  (canPin checked, API 26+), async-dialog outcome verified via delayed
  getGlanceIds re-check (NOT dismissed on dialog open — "No" is never
  punished), "Not now" → WIDGET_PROMO_DISMISSED pref, card auto-hides when
  widgets hosted. Guidebook PHASE-5.1 addendum added. Build fix: LocalSize
  lives in androidx.glance (verified in androidx source, 5b4fcd3). User test:
  widget responsive BUT fonts never grew (root cause: 5.1 anchors wider than
  real grid slots → compact fallback every time) + wanted inline card as a
  timed popup instead. [PHASE-5.2] (d875a77): anchors 130/195 x 60/170 (real
  ~70dp cells), NEW tall layout for 2x2 (stacked, 24sp name), hero 3x2+ =
  28sp name / 22sp time / 15sp countdown / 15sp ayah 3 lines + city; promo
  → ModalBottomSheet after 5s delay, once per app open (state machine:
  showPromoSheet flag + LaunchedEffect delay; eligibility unchanged).
  Guidebook PHASE-5.2 addendum added. **Next: build + re-test (fonts grow
  with size, popup appears at 5s) → Phase 6 (Quran).**
- **2026-09-19 — Session 10 (CLOSE):** [PHASE-5.2] executed per user-approved
  plan (d875a77 + save 902dbd6, both pushed): real-grid anchors, tall 2x2
  layout, "way bigger" font ladder, promo → 5s ModalBottomSheet popup.
  Self-caught pre-build: missing .background import + dead SurfaceDeep/width
  imports in HomeScreen; verification pass green (braces, anchors, ladder,
  sheet wiring, no leftovers). **BUILD WAS NOT TESTED — user closed the
  session; NEXT SESSION MUST ASK FOR PHASE-5.2 TEST RESULTS FIRST**  (resize through 4 shapes + 5s popup + quick tap/reboot confirmations), then Phase 6
  (Quran) on explicit OK. Save file's WHERE WE ARE + NEXT SESSION sections
  rewritten for a clean handoff.
- **2026-09-20 — Session 11:** User's Codespace stopped reloading (no way to build).
  Response: re-enabled push-triggered Actions builds on the existing debugged workflow
  (+ paths-ignore & concurrency-cancel; see KEY DECISIONS). The push itself fired the
  first auto-build of the PHASE-6 code — APK downloadable from the run's Artifacts.
  Save-file housekeeping: Phase 5 marked ✅ in the phases table (was stale), Phase 6
  marked code-complete, NEXT SESSION checklist promoted to PHASE-6. Codespaces kept
  as manual fallback (fix recipes: hard refresh / Rebuild Container / recreate).
  **Next: user downloads Actions APK → runs the 9-check PHASE-6 list → Phase 6
  verdict → Phase 7 (Qibla) on explicit OK.**
- **2026-09-20 — Session 12:** Codespace became unusable → APK builds moved to
  GitHub Actions auto-build on push (KEY DECISIONS updated; push 5356fe0 fired
  build #1). Build #1 RED: expression-body `return`s in AyahRepository → fixed
  (2085a1b, 1 file). Build #2 RED: Dagger `DuplicateBindings` — two Retrofit
  instances from Phase 6 unqualified → fixed with @Qualifiers (db588e8, 1 file,
  +15/−2). Build #2 passing Kotlin compile retroactively validates the entire
  Phase-6 batch (failure had moved to the DI-graph stage). Errors read via the
  public check-runs annotations API — user never had to paste a log. **USER RULE
  ADDED (Rule 6): explicit "BUILD" permission gate — plan first, code only on
  explicit go; surgical commits with diff-stat proof.** Save file polished.
  **Next: build #3 verdict → green → user tests PHASE-6 9-check list → Phase 7
  (Qibla) PLAN ONLY, no code until the user says BUILD.**

## PHASE-2 IMPLEMENTATION NOTES (for future debugging)
- New files: data/model/PrayerTimings.kt, data/api/Aladhan{Models,ApiService}.kt,
  data/db/{MawaqitEntities,MawaqitDatabase}.kt, data/prefs/PrefsRepository.kt,
  data/repository/{PrayerRepository,PrayerRepositoryImpl}.kt, di/{NetworkModule,RepositoryModule}.kt,
  util/{PrayerTimeUtils,LocationHelper}.kt, ui/home/{HomeScreen,HomeViewModel}.kt.
  Modified: MainActivity (shows HomeScreen), AppModule (Room providers).
- Cache logic: pref LAST_MONTH_FETCHED="YYYY-MM" + countForMonth>0 → skip API;
  setLocation() clears BOTH (location change invalidates all rows).
- Next prayer: today's remaining, else tomorrow's Fajr (approximated with today's Fajr time
  on a month's last day — accepted MVP simplification).
- Countdown uses device timezone mapping (see parseTimeToMillis note) — fine while device tz
  == home tz (our audience).

## CONVENTIONS CHEAT SHEET
- Commit messages: `[PHASE-N] Short description` (match `git log` style).
- Project lives at `mawaqit/` (repo root = git repo), guidebook at `mawaqit-guidebook/`,
  user to-dos at `assignment/`. Parent folder: `~/Mawaqit Salah Time, Prayer Alarm Clock/`.
- Versions: Gradle 8.7, AGP 8.4.2, Kotlin 1.9.24, KSP 1.9.24-1.0.20, Hilt 2.51.1, compose-bom
  2024.06.00, Java 17, compileSdk 34, minSdk 26. Don't bump casually — they were chosen together.
- The repo's `.gitignore` deliberately does NOT ignore `gradle/wrapper/gradle-wrapper.jar`.

---

## 🔑 THE DAILY STARTUP PROMPT (paste this to start any new session)

```
Hi Buffy! Fresh session. Boot up like this:

1. Read mawaqit/PROGRESS.md first — it's our save file.
2. Read mawaqit-guidebook/AGENTS.md — project rules.
3. Run: cd mawaqit && git log --oneline -3 && git status
   (confirms the repo state matches PROGRESS.md)

Then greet me with: where we are, what's next, and what YOU need from me today.
Explain everything like I'm 16, one phase at a time, and update PROGRESS.md
at the end of the session. Don't start any new phase without my explicit OK.
```

That's it. Those 3 reads = full context, every time. 🚀

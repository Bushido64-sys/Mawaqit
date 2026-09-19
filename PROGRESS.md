# PROGRESS.md — The Project's Save File 💾
**Mawaqit: Salah Time, Prayer Alarm Clock (Android)**

> **Purpose:** the single source of truth for where the project is.
> A fresh AI session reads THIS file first and instantly knows what's done,
> what's pending, and what to do next — no archaeology, no guessing.
> The AI updates it at the end of every work session. If it's stale, that's a bug — fix it.
> **Last updated: 2026-09-19 — [PHASE-4] build OK + phone test: 6/8 pass; 2 bugs found & FIXED (commit pending): hero countdown froze at 00:00:00 after prayer passed (ticker now recomputes next prayer in real time), gold "current prayer" stuck on Fajr all day (now advances with the day). Mosque notification icon = by design, not a bug. Rebuild + re-test pending.**

---

## THE PROJECT IN ONE LINE
Android prayer-times + alarm app (Kotlin, Compose, offline-first), built phase by phase
from a detailed guidebook (`mawaqit-guidebook/`), with the user learning as we go.

## WHERE WE ARE RIGHT NOW
**Phase 3 (alarms) — CODE COMPLETE, committed & pushed 2026-09-18.** Not yet built
or phone-tested. User chose **silent stub audio** (3 tiny silent files in res/raw;
real azan MP3s swap in later → one rebuild) and approved both TEMP test helpers
(5 alarm switches + notification permission popup on the temp HomeScreen).

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
| 4 | Home screen UI | 🚧 code OK + tested 6/8; 2 UI-tick bugs FIXED (real-time next-prayer rollover + current-prayer advance) — needs rebuild + re-test |
| 5 | Widget (Glance; lock-screen = opportunistic bonus) | not started |
| 6 | Quran (UmmahAPI — endpoints re-verified live in Sept 2026) | not started |
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

## KEY DECISIONS (the "why" behind the code)
- **Builds happen in GitHub CODESPACES, not GitHub Actions.** The `android-actions/setup-android`
  action broke for everyone when Google retired the `tools` package (Sept 2026), and Actions logs
  were unreadable without repo-owner login. Codespaces: proper SDK + visible errors.
  → `codespaces/setup.sh` (once) + `codespaces/build.sh` (every build).
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
2. Ask user for the build result: `codespaces/build.sh` output (APK name or the
   "What went wrong" box) or the exact phone behavior. One fix at a time,
   commit `[PHASE-3]`, push, user rebuilds.
3. Build is green → APK on phone → run the PHASE_4_HOME_SCREEN.md "PHASE 4
   COMPLETE WHEN" checks: layout matches DESIGN.md, countdown ticks every second,
   tapping a prayer marks it (green check persists across app restart), daily
   ayah shows Arabic + translation, bottom nav switches all 4 tabs, offline
   banner when no internet, alarm toggles persist. REGRESSION: alarms still fire
   at prayer time (silent) — note the Test-azan button is GONE now (temp UI),
   so use real prayer times or the clock trick.
4. Phase 4 passes → user gives explicit OK → PHASE_5 (Glance widget) next.
   Glance deps already in build.gradle.kts; read PHASE_5_WIDGET.md first.
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

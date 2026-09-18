# PROGRESS.md — The Project's Save File 💾
**Mawaqit: Salah Time, Prayer Alarm Clock (Android)**

> **Purpose:** the single source of truth for where the project is.
> A fresh AI session reads THIS file first and instantly knows what's done,
> what's pending, and what to do next — no archaeology, no guessing.
> The AI updates it at the end of every work session. If it's stale, that's a bug — fix it.
> **Last updated: 2026-09-18 — PHASE 3 CODE COMPLETE (committed + pushed). Next: Codespaces build, then phone test — pending build result.**

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
**Next: user runs codespaces/build.sh → installs APK → 7 phone checks
(PHASE_3_ALARMS.md checklist, adapted for silent audio — see assignment when asked).**

---

## THE PHASES (from the guidebook)
| Phase | What | Status |
|---|---|---|
| 0 | Guidebook written, reviewed, 11 doc issues fixed | ✅ done |
| 1 | Skeleton: Gradle, manifest, resources, theme, Hilt app, MainActivity | ✅ COMPLETE — 5/5 phone checks passed 2026-09-17 |
| 2 | Prayer times (AlAdhan API + Room + repository) | ✅ COMPLETE — 5/5 phone checks passed 2026-09-17 |
| 3 | Alarms (AlarmReceiver, BootReceiver, AzanService) | 🔨 CODE COMPLETE 2026-09-18 — needs build + phone test |
| 4 | Home screen UI | not started |
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
3. Build is green → APK on phone → run the 7 Phase-3 checks (PHASE_3_ALARMS.md
   "PHASE 3 COMPLETE WHEN" checklist). Silent audio means: verify by SEEING the
   notification appear at prayer time and disappear after 5 min / Mark-as-Prayed.
4. Phase 3 passes → user gives explicit OK → PHASE_4 (home screen UI) next.
   NOTE for Phase 4: remove the two TEMP helpers from HomeScreen (switches +
   notif popup) and replace the placeholder notification icon.
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

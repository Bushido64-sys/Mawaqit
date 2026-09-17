#!/usr/bin/env bash
# ============================================================
# Mawaqit — Codespaces BUILD (run this every time you want an APK)
#   bash codespaces/build.sh
#
# ⚠️ Refuses to build unless Java 17 is active — the codespace default
# Java (25) crashes our Kotlin compiler. Run codespaces/setup.sh once
# if this script says Java 17 is missing.
#
# On success: prints the APK path, named with the commit hash.
# On failure: prints the "What went wrong" box — copy it into the chat.
# ============================================================
set -euo pipefail

cd "$(dirname "$0")/.."   # always run from the repo root

# ---------- helper: find a Java 17 (same logic as setup.sh) ----------
find_java17() {
  local d
  for d in "$HOME"/jdks/*/; do
    if [ -x "$d/bin/java" ] && "$d/bin/java" -version 2>&1 | grep -q 'version "17'; then
      echo "${d%/}"; return 0
    fi
  done
  if [ -d "$HOME/.sdkman/candidates/java" ]; then
    for d in "$HOME"/.sdkman/candidates/java/17*/; do
      if [ -x "$d/bin/java" ] && "$d/bin/java" -version 2>&1 | grep -q 'version "17'; then
        echo "${d%/}"; return 0
      fi
    done
  fi
  for d in /usr/lib/jvm/java-17-openjdk-* /usr/lib/jvm/temurin-17-jdk-*; do
    if [ -x "$d/bin/java" ] && "$d/bin/java" -version 2>&1 | grep -q 'version "17'; then
      echo "$d"; return 0
    fi
  done
  return 1
}

# ---------- 1. FORCE Java 17 ----------
if ! java -version 2>&1 | grep -q 'version "17'; then
  # current java is NOT 17 → switch to a discovered 17 (setup.sh installed one)
  if JAVA17_HOME="$(find_java17)"; then
    export JAVA_HOME="$JAVA17_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
  else
    echo "❌ Java 17 not found on this machine."
    echo "   Fix:  bash codespaces/setup.sh     (then run this script again)"
    exit 1
  fi
fi
echo "-- Building with: $(java -version 2>&1 | head -n1)"

# ---------- 2. SDK env (in case setup ran in another terminal) ----------
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_HOME="$SDK"
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"

chmod +x ./gradlew

# ---------- 3. Build ----------
echo "=========================================="
echo " Mawaqit — building debug APK..."
echo "=========================================="
LOG=/tmp/mawaqit-build.log
if ./gradlew assembleDebug --stacktrace 2>&1 | tee "$LOG"; then
  echo ""
  echo "=========================================="
  echo " ✅ BUILD SUCCESSFUL"
  echo "=========================================="
  echo ""
  # Stamp the commit hash into the APK file name so you can always
  # tell exactly which build this is (same ID shown inside the app).
  SHA="$(git rev-parse --short HEAD 2>/dev/null || echo dev)"
  APK_DIR="app/build/outputs/apk/debug"
  APK="$(ls -t "$APK_DIR"/*.apk 2>/dev/null | head -n1 || true)"
  if [ -n "$APK" ]; then
    NAMED="$APK_DIR/Mawaqit-1.0.0-$SHA-debug.apk"
    [ "$APK" != "$NAMED" ] && mv -f "$APK" "$NAMED" 2>/dev/null || true
    APK="$NAMED"
  fi
  echo " Your APK (the number after 1.0.0- is the build ID,"
  echo " it matches the commit this APK was built from):"
  ls -lh "$APK" | awk '{print "   📱 " $9 "  (" $5 ")"}'
  echo ""
  echo " Download it: left sidebar → Explorer → navigate to"
  echo " app/build/outputs/apk/debug/ → right-click the .apk → Download"
else
  echo ""
  echo "=========================================="
  echo " ❌ BUILD FAILED — here is the important part"
  echo "=========================================="
  grep -n -A 20 "What went wrong" "$LOG" | head -60 || true
  grep -nE "^e: |error:|Caused by:" "$LOG" | head -30 || true
  echo ""
  echo "☝️ Copy everything in the box above (the whole failure block)"
  echo "   and paste it into the chat — I'll fix it."
  exit 1
fi

#!/usr/bin/env bash
# ============================================================
# Mawaqit — Codespaces BUILD (run this every time you want an APK)
#   bash codespaces/build.sh
# At the end it prints the exact APK file name, which contains
# the commit hash it was built from (e.g. Mawaqit-1.0.0-e987c71-debug.apk).
# If the build fails, it prints the "What went wrong" section —
# copy that and paste it into the chat.
# ============================================================
set -euo pipefail

cd "$(dirname "$0")/.."   # always run from the repo root

# make sure the environment is set up even if setup.sh ran in another terminal
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_HOME="$SDK"
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"
if [ -d "$HOME/.sdkman/candidates/java" ]; then
  V17="$(ls "$HOME/.sdkman/candidates/java" 2>/dev/null | grep -E '^17\.' | head -n1 || true)"
  if [ -n "$V17" ] && ! java -version 2>&1 | grep -q 'version "17'; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/$V17"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi

chmod +x ./gradlew

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
  # Gradle prints failures under "What went wrong" / "FAILURE:"
  grep -n -A 20 "What went wrong" "$LOG" | head -60 || true
  grep -nE "^e: |error:|Caused by:" "$LOG" | head -30 || true
  echo ""
  echo "☝️ Copy everything in the box above (the whole failure block)"
  echo "   and paste it into the chat — I'll fix it."
  exit 1
fi

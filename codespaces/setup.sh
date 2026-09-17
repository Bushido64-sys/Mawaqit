#!/usr/bin/env bash
# ============================================================
# Mawaqit — GitHub Codespaces SETUP (run this ONCE per codespace)
#   bash codespaces/setup.sh
#
# ⚠️ WHY WE FORCE JAVA 17:
# The codespace's default Java is brand-new (e.g. 25) and our Kotlin
# compiler (1.9.24) cannot even READ its version number — it crashed with
# "IllegalArgumentException: 25.0.4.1". Java 17 is what Android builds
# expect. So this script installs Temurin 17 into ~/jdks (no sudo needed)
# and makes BOTH terminals and the build use it.
#
# Safe to re-run — skips work that's already done.
# ============================================================
set -euo pipefail

echo "=========================================="
echo " Mawaqit — Codespaces setup"
echo "=========================================="

# ---------- helper: find an existing Java 17 anywhere ----------
find_java17() {
  # 1) previously-installed Temurin from this script
  local d
  for d in "$HOME"/jdks/*/; do
    if [ -x "$d/bin/java" ] && "$d/bin/java" -version 2>&1 | grep -q 'version "17'; then
      echo "${d%/}"; return 0
    fi
  done
  # 2) sdkman-managed JDKs (codespace images ship several)
  if [ -d "$HOME/.sdkman/candidates/java" ]; then
    for d in "$HOME"/.sdkman/candidates/java/17*/; do
      if [ -x "$d/bin/java" ] && "$d/bin/java" -version 2>&1 | grep -q 'version "17'; then
        echo "${d%/}"; return 0
      fi
    done
  fi
  # 3) system-installed (apt) locations
  for d in /usr/lib/jvm/java-17-openjdk-* /usr/lib/jvm/temurin-17-jdk-*; do
    if [ -x "$d/bin/java" ] && "$d/bin/java" -version 2>&1 | grep -q 'version "17'; then
      echo "$d"; return 0
    fi
  done
  return 1
}

# ---------- 1. Java 17: find it or install it ----------
if JAVA17_HOME="$(find_java17)"; then
  echo "-- Found Java 17 at: $JAVA17_HOME"
else
  echo "-- No Java 17 found. Downloading Temurin 17 (~190MB, one-time)..."
  mkdir -p "$HOME/jdks"
  TMPTGZ="$(mktemp /tmp/jdk17.XXXXXX.tar.gz)"
  if curl -fsSL -o "$TMPTGZ" \
      "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"; then
    tar -xzf "$TMPTGZ" -C "$HOME/jdks"
    rm -f "$TMPTGZ"
    JAVA17_HOME="$(find_java17)" || true
  fi
  if [ -z "${JAVA17_HOME:-}" ]; then
    echo "-- Download failed, trying apt (needs sudo)..."
    sudo apt-get update -y -qq
    sudo apt-get install -y -qq openjdk-17-jdk
    JAVA17_HOME="$(find_java17)" || true
  fi
  if [ -z "${JAVA17_HOME:-}" ]; then
    echo "❌ Could not install Java 17. Paste this whole output into the chat."
    exit 1
  fi
  echo "-- Installed Java 17 at: $JAVA17_HOME"
fi

# make it the Java for THIS terminal and every future one
export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
if ! grep -q "MAWAQIT_JAVA17" "$HOME/.bashrc" 2>/dev/null; then
  {
    echo "# MAWAQIT_JAVA17 — Java 17 for the Mawaqit Android build"
    echo "export JAVA_HOME=\"$JAVA17_HOME\""
    echo 'export PATH="$JAVA_HOME/bin:$PATH"'
  } >> "$HOME/.bashrc"
fi
echo "-- Active Java: $(java -version 2>&1 | head -n1)"

# ---------- 2. Android SDK command-line tools ----------
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_HOME="$SDK"
if [ ! -d "$SDK/cmdline-tools/latest" ]; then
  echo "-- Downloading Android command-line tools..."
  mkdir -p "$SDK/cmdline-tools"
  TMPZIP="$(mktemp /tmp/cmdtools.XXXXXX.zip)"
  curl -fsSL -o "$TMPZIP" \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  unzip -qo "$TMPZIP" -d "$SDK/cmdline-tools"
  rm -f "$TMPZIP"
  [ -d "$SDK/cmdline-tools/latest" ] || mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"

if ! grep -q "ANDROID_HOME" "$HOME/.bashrc" 2>/dev/null; then
  {
    echo "export ANDROID_HOME=\"$SDK\""
    echo 'export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"'
  } >> "$HOME/.bashrc"
fi

# ---------- 3. Licenses + exact packages the project uses ----------
echo "-- Accepting Android SDK licenses..."
yes | sdkmanager --licenses >/dev/null 2>&1 || true
echo "-- Installing SDK packages (platform-tools, android-34, build-tools 34.0.0)..."
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null

echo ""
echo "=========================================="
echo " ✅ Setup complete! (one-time)"
echo " Active Java: $(java -version 2>&1 | head -n1)"
echo " Next:  bash codespaces/build.sh"
echo "=========================================="

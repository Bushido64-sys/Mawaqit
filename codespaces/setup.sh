#!/usr/bin/env bash
# ============================================================
# Mawaqit — GitHub Codespaces SETUP (run this ONCE per codespace)
#   bash codespaces/setup.sh
# Installs: Java 17 (if missing) + the exact Android SDK pieces
# our project needs. Safe to re-run — it skips what's already done.
# ============================================================
set -euo pipefail

echo "=========================================="
echo " Mawaqit — Codespaces setup"
echo "=========================================="

# ── 1. Java 17 ────────────────────────────────────────────────
# Codespaces images ship several JDKs via sdkman; prefer a 17 from there,
# fall back to apt only if none exists.
JAVA_OK=false
if command -v java >/dev/null 2>&1; then
  if java -version 2>&1 | grep -q 'version "17'; then JAVA_OK=true; fi
fi
if [ "$JAVA_OK" = false ] && [ -d "$HOME/.sdkman/candidates/java" ]; then
  V17="$(ls "$HOME/.sdkman/candidates/java" 2>/dev/null | grep -E '^17\.' | head -n1 || true)"
  if [ -n "$V17" ]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/$V17"
    export PATH="$JAVA_HOME/bin:$PATH"
    JAVA_OK=true
    # remember for future terminals
    {
      echo "export JAVA_HOME=\"$JAVA_HOME\""
      echo 'export PATH="$JAVA_HOME/bin:$PATH"'
    } >> "$HOME/.bashrc"
  fi
fi
if [ "$JAVA_OK" = false ]; then
  echo "-- Installing Java 17 via apt..."
  sudo apt-get update -y -qq
  sudo apt-get install -y -qq openjdk-17-jdk
  export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
  {
    echo "export JAVA_HOME=\"$JAVA_HOME\""
    echo 'export PATH="$JAVA_HOME/bin:$PATH"'
  } >> "$HOME/.bashrc"
fi
echo "-- Java: $(java -version 2>&1 | head -n1)"

# ── 2. Android SDK command-line tools ─────────────────────────
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
  # sdkmanager expects the folder to be named "latest"
  [ -d "$SDK/cmdline-tools/latest" ] || mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"

# remember ANDROID_HOME for future terminals (Gradle looks for it)
if ! grep -q "ANDROID_HOME" "$HOME/.bashrc" 2>/dev/null; then
  {
    echo "export ANDROID_HOME=\"$SDK\""
    echo 'export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"'
  } >> "$HOME/.bashrc"
fi

# ── 3. Licenses + exact packages the project uses ─────────────
echo "-- Accepting Android SDK licenses..."
yes | sdkmanager --licenses >/dev/null 2>&1 || true
echo "-- Installing SDK packages (platform-tools, android-34, build-tools 34.0.0)..."
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null

echo ""
echo "=========================================="
echo " ✅ Setup complete! (one-time)"
echo " Next:  bash codespaces/build.sh"
echo "=========================================="

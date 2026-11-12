#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
echo "DaniM Auto Warp: comprobación de entorno Android"
if [[ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]]; then
  echo "ERROR: falta ANDROID_HOME o ANDROID_SDK_ROOT (Android SDK 35 requerido)" >&2
  exit 2
fi
SDK="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
if [[ ! -d "$SDK/platforms/android-35" ]]; then
  echo "ERROR: falta Android SDK Platform 35 en $SDK" >&2
  exit 2
fi
if [[ -x ./gradlew ]]; then
  ./gradlew --no-daemon :app:assembleDebug
elif command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon :app:assembleDebug
else
  echo "ERROR: falta Gradle o el wrapper gradlew; abrir proyecto en Android Studio y sincronizar Gradle" >&2
  exit 2
fi
echo "APK esperado: app/build/outputs/apk/debug/app-debug.apk"

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]]; then
  echo "ERROR: Define ANDROID_HOME o ANDROID_SDK_ROOT con la ruta del Android SDK" >&2
  exit 2
fi
SDK="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
if [[ ! -d "$SDK/platforms/android-35" ]]; then
  echo "ERROR: Falta Android SDK Platform 35 en $SDK" >&2
  exit 2
fi
if [[ -x ./gradlew ]]; then
  ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
elif command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
else
  echo "ERROR: Gradle no está instalado y este proyecto no incluye gradlew" >&2
  exit 2
fi

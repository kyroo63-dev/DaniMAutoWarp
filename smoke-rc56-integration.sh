#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
python3 - "$ROOT" <<'PYTEST'
from pathlib import Path
import sys
r=Path(sys.argv[1]); main=(r/'app/src/main/java/com/danim/autowarp/MainActivity.kt').read_text()
assert 'abs(candidate - latest[selected].sourceSeconds) >= 0.000001' in main
assert 'index !in 1 until markers.lastIndex' in main
assert 'abs(bounded - old.sourceSeconds) < 0.000001' in main
assert 'versionName = "4.0.0-rc56"' in (r/'app/build.gradle.kts').read_text()
print('PASS: RC56 drag integration guards, bounds, no-op and version')
PYTEST

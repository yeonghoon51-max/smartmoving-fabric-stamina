#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "[1/2] Trying full Fabric build (runnable mod jar)..."
if gradle --no-daemon --console=plain clean build --project-dir "$PROJECT_DIR"; then
  echo "Full build succeeded."
  jars=(build/libs/*.jar)
  for jar in "${jars[@]}"; do
    case "$jar" in
      *-sources.jar|*stub*.jar) ;;
      *) echo "MOD_JAR=$jar"; exit 0 ;;
    esac
  done
  echo "Full build completed but no runnable jar found in build/libs" >&2
  exit 1
fi

echo "[2/2] Full build failed, falling back to offline stub build..."
gradle --no-daemon --console=plain clean build \
  -PuseStubDeps=true -DuseStubLoom=true --offline --project-dir "$PROJECT_DIR"

stub_jars=(build/libs/stub/*-stub.jar)
if [[ -f "${stub_jars[0]}" ]]; then
  echo "STUB_JAR=${stub_jars[0]}"
  exit 0
fi

echo "Stub build completed but no stub jar found in build/libs/stub" >&2
exit 1

#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PIN="ea0c4c2c85fb183ec0fbd30a1af4e4275a18a064"
DIR="$ROOT/third_party/llama.cpp"
if [[ ! -d "$DIR/.git" ]]; then
  mkdir -p "$DIR"
  git -C "$DIR" init -q
  git -C "$DIR" remote add origin https://github.com/ggml-org/llama.cpp.git
fi
if ! git -C "$DIR" rev-parse --verify "$PIN^{commit}" >/dev/null 2>&1; then
  git -C "$DIR" fetch --depth=1 --filter=blob:none origin "$PIN"
fi
git -C "$DIR" checkout --detach -q "$PIN"
# O wrapper, catalogo Gradle e modulo nativo sao provenientes da MESMA revisao fixada.
mkdir -p "$ROOT/gradle"
cp "$DIR/examples/llama.android/gradle/libs.versions.toml" "$ROOT/gradle/libs.versions.toml"
cp -R "$DIR/examples/llama.android/gradle/wrapper" "$ROOT/gradle/"
cp "$DIR/examples/llama.android/gradlew" "$ROOT/gradlew"
chmod +x "$ROOT/gradlew"
# Limita o binario nativo a ARM64 para otimizar APK e compilacao.
python3 - "$DIR/examples/llama.android/lib/build.gradle.kts" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1]); data=p.read_text()
a='abiFilters += listOf("arm64-v8a", "x86_64")'
b='abiFilters += listOf("arm64-v8a")'
if a in data: p.write_text(data.replace(a,b))
elif b not in data: raise SystemExit('Falha: upstream mudou o filtro ABI. Inspecione antes de continuar.')
PY
printf 'Dependencia llama.cpp verificada: %s\n' "$PIN"

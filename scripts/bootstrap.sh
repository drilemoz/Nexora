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
# Ajustes minimos sobre a revisao fixa; falhar cedo se upstream mudar o trecho esperado.
# 4096 reduz o teto de memoria KV por contexto, ao custo de menos tokens para PDFs longos.
# O limite de geracao nao pode contar os tokens de entrada pela segunda vez.
python3 - "$DIR/examples/llama.android/lib/src/main/cpp/ai_chat.cpp" <<'PY_NATIVE'
from pathlib import Path
import sys
source = Path(sys.argv[1])
code = source.read_text()
patches = (
    ('constexpr int   DEFAULT_CONTEXT_SIZE    = 8192;',
     'constexpr int   DEFAULT_CONTEXT_SIZE    = 4096;'),
    ('current_position += user_prompt_size;\n    stop_generation_position = current_position + user_prompt_size + n_predict;',
     'current_position += (int) user_tokens.size();\n    stop_generation_position = current_position + n_predict;'),
    ('LOGd("%s: User prompt received: \\n%s", __func__, user_prompt);',
     'LOGd("%s: User prompt received (text omitted)", __func__);'),
    ('LOGd("%s: System prompt received: \\n%s", __func__, system_prompt);',
     'LOGd("%s: System prompt received (text omitted)", __func__);'),
    ('for (auto id: user_tokens) {\n        LOGv("token: `%s`\\t -> `%d`", common_token_to_piece(g_context, id).c_str(), id);\n    }',
     '// Nexora: sem log de cada token de entrada (desempenho e privacidade).'),
    ('for (auto id: system_tokens) {\n        LOGv("token: `%s`\\t -> `%d`", common_token_to_piece(g_context, id).c_str(), id);\n    }',
     '// Nexora: sem log de cada token do sistema.'),
)
for before, after in patches:
    if code.count(before) != 1:
        raise SystemExit(f'Patch nativo nao aplicavel (encontrado {code.count(before)}): {before[:80]}')
    code = code.replace(before, after)
source.write_text(code)
PY_NATIVE
printf 'Dependencia llama.cpp verificada e patches conservadores aplicados: %s\n' "$PIN"
# Instrumentacao vinculada ao pin acima: status real de termino e contadores de tokens.
python3 "$ROOT/scripts/patch_llama_metrics.py" "$DIR/examples/llama.android"
python3 "$ROOT/scripts/patch_llama_preferences.py" "$DIR/examples/llama.android"

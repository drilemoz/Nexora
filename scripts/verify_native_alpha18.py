#!/usr/bin/env python3
"""Verificação estática do motor já preparado pelo bootstrap (não substitui build/Android)."""
from pathlib import Path
import sys

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / 'third_party/llama.cpp/examples/llama.android'
cpp = (root/'lib/src/main/cpp/ai_chat.cpp').read_text()
api = (root/'lib/src/main/java/com/arm/aichat/InferenceEngine.kt').read_text()
impl = (root/'lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt').read_text()

checks = {
    'no plaintext formatted messages': 'Formatted and added %s message' not in cpp,
    'no token contents in Logcat': 'cached_token_chars.c_str(), new_token_chars.c_str()' not in cpp,
    'no plaintext prompts': 'User prompt received: \\n%s' not in cpp and 'System prompt received: \\n%s' not in cpp,
    'predict-limit counter': 'nx_generated_tokens >= nx_predict_limit' in cpp,
    'real positions after KV shift': 'batch_position = current_position;' in cpp and 'current_position = batch_position;' in cpp,
    'no prompt double-count': 'current_position += (int) user_tokens.size();' not in cpp,
    'KV reset implementation': 'common_sampler_reset(g_sampler);' in cpp and 'nativeResetConversation' in cpp,
    'partial cleanup': 'if (g_context != nullptr) llama_free(g_context);' in cpp and 'g_context = nullptr;' in cpp,
    'reset API': 'suspend fun resetConversation()' in api and 'override suspend fun resetConversation()' in impl,
    'error cleanup unload': 'Freeing partial native resources after failure' in impl and 'unload()' in impl,
}
for label, valid in checks.items():
    print(('OK  ' if valid else 'FAIL') + label)
if not all(checks.values()):
    raise SystemExit(1)

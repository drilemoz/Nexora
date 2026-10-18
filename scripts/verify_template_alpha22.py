#!/usr/bin/env python3
"""Asserções estáticas do patch já aplicado; valida lógica/âncoras, não compila JNI."""
from pathlib import Path
import sys
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parent.parent/'third_party/llama.cpp/examples/llama.android'
cpp=(root/'lib/src/main/cpp/ai_chat.cpp').read_text()
api=(root/'lib/src/main/java/com/arm/aichat/InferenceEngine.kt').read_text()
impl=(root/'lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt').read_text()
checks={
 'GGUF automatic': 'llama_model_chat_template(g_model, nullptr)' in cpp and 'common_chat_templates_init(g_model, "")' in cpp,
 'multiturn roles': all(x in cpp for x in ['"system", "nx_sys_probe"','"user", "nx_user_probe"','"assistant", "nx_assistant_probe"','"user", "nx_second_probe"']),
 'jinja engine': 'input.use_jinja = true;' in cpp and '/* use_jinja */ true' in cpp,
 'fail closed': 'nx_template_present && !nx_template_safe' in cpp and cpp.count('return 7;')>=2,
 'no double BOS': 'user_tokens[0] != bos' in cpp and 'system_tokens[0] != bos' in cpp,
 'no double format': 'const bool has_chat_template = nx_template_present && nx_template_safe;' in cpp,
 'verify without generation': 'nx_validate_template()' in cpp and 'nativeTemplateInfo' in cpp,
 'API': 'suspend fun templateReport' in api and 'override suspend fun templateReport' in impl,
 'privacy': 'nx_template_report' in cpp and 'nx_template_error' in cpp,
}
for name,ok in checks.items(): print(('OK  ' if ok else 'FAIL ')+name)
if not all(checks.values()): raise SystemExit(1)

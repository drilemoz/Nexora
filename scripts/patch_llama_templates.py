#!/usr/bin/env python3
"""alpha.22: inspecao real do GGUF, template por modelo e verificacao sintética (pin obrigatório).
Executar somente depois de patches alpha.18; falha fechada se upstream alterar as ancorações.
"""
from pathlib import Path
import sys
root=Path(sys.argv[1]); cpp=root/'lib/src/main/cpp/ai_chat.cpp'; api=root/'lib/src/main/java/com/arm/aichat/InferenceEngine.kt'; impl=root/'lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt'
def one(data,before,after,label):
    n=data.count(before)
    if n!=1: raise SystemExit(f'alpha22 template {label}: esperava 1 ocorrência, encontrei {n}')
    return data.replace(before,after)
s=cpp.read_text()
s=one(s, '#include <string>', '#include <string>\n#include <algorithm>\n#include <exception>\n#include <utility>', 'includes')
s=one(s,'static common_sampler                   * g_sampler;', '''static common_sampler                   * g_sampler;
// Nenhum campo abaixo contém prompts, textos de GGUF, caminhos ou templates completos.
static bool nx_template_present = false;
static bool nx_template_safe = false;
static bool nx_template_checked = false;
static bool nx_template_system_supported = true;
static const char * nx_template_family = "desconhecido";
static const char * nx_template_error = "nao_verificado";
static const char * nx_template_origin = "fallback";

// Classificação descritiva baseada em marcadores, nunca é usada para selecionar um template.
static const char * nx_identify_template(const std::string & source) {
    if (source.find("<start_of_turn>") != std::string::npos) return "Gemma";
    if (source.find("<|start_header_id|>") != std::string::npos) return "Llama";
    if (source.find("<|im_start|>") != std::string::npos) return "ChatML_Qwen_SmolLM";
    if (source.find("<|user|>") != std::string::npos &&
        source.find("<|assistant|>") != std::string::npos) return "Phi";
    return "Jinja_GGUF";
}

// Nenhuma geração/inferência: só aplicação do template e tokenização de frases sintéticas.
static bool nx_validate_template() {
    nx_template_checked = true;
    nx_template_safe = false;
    nx_template_system_supported = true;
    if (!g_model || !g_context) { nx_template_error = "motor_indisponivel"; return false; }
    if (!nx_template_present || !g_chat_templates) {
        nx_template_error = "sem_template_gguf";
        return false;
    }
    try {
        std::vector<common_chat_msg> messages;
        for (const auto & item : {std::pair<const char *, const char *>("system", "nx_sys_probe"),
                                  {"user", "nx_user_probe"},
                                  {"assistant", "nx_assistant_probe"},
                                  {"user", "nx_second_probe"}}) {
            common_chat_msg msg;
            msg.role = item.first;
            msg.content = item.second;
            messages.push_back(std::move(msg));
        }
        // Primeiro tenta system/user/assistant/user. Alguns GGUF não suportam system:
        // neste caso valida sem system e bloqueia especificamente setSystemPrompt.
        for (const bool with_system : {true, false}) {
            auto probe = messages;
            if (!with_system) {
                probe.erase(probe.begin());
                common_chat_msg last;
                last.role = "assistant";
                last.content = "nx_last_probe";
                probe.push_back(std::move(last));
                common_chat_msg new_user;
                new_user.role = "user";
                new_user.content = "nx_final_probe";
                probe.push_back(std::move(new_user));
            }
            try {
                common_chat_templates_inputs input;
                input.use_jinja = true;
                input.messages = {probe[0]};
                input.add_generation_prompt = false;
                auto first = common_chat_templates_apply(g_chat_templates.get(), input).prompt.str();
                input.messages = {probe[0], probe[1]};
                input.add_generation_prompt = false;
                auto second = common_chat_templates_apply(g_chat_templates.get(), input).prompt.str();
                input.messages.assign(probe.begin(), probe.end() - 1);
                input.add_generation_prompt = false;
                auto third = common_chat_templates_apply(g_chat_templates.get(), input).prompt.str();
                input.messages.push_back(probe.back());
                input.add_generation_prompt = true;
                auto fourth = common_chat_templates_apply(g_chat_templates.get(), input).prompt.str();
                if (first.empty() || second.size() <= first.size() ||
                    second.compare(0, first.size(), first) != 0 ||
                    fourth.size() <= third.size() || fourth.compare(0, third.size(), third) != 0) {
                    nx_template_error = "prefixo_multiturno";
                    continue;
                }
                auto tokens = common_tokenize(g_context, fourth, false, true);
                if (tokens.empty()) { nx_template_error = "tokenizacao_vazia"; continue; }
                nx_template_error = with_system ? "ok" : "ok_sem_system";
                nx_template_safe = true;
                nx_template_system_supported = with_system;
                return true;
            } catch (const std::exception &) {
                nx_template_error = "template_incompativel";
            }
        }
        return false;
    } catch (const std::exception &) {
        nx_template_error = "template_incompativel";
        return false;
    } catch (...) {
        nx_template_error = "validacao_falhou";
        return false;
    }
}

// Protocolo compacto formado APENAS por enums internos e SHA de compilação.
static std::string nx_template_report() {
    return std::string(nx_template_family) + "|" + nx_template_origin + "|" +
        (nx_template_safe ? "ativo" : (nx_template_present ? "incompativel" : "nao_aplicado")) + "|" +
        (nx_template_checked ? "verificado" : "nao_verificado") + "|" + nx_template_error +
        "|ea0c4c2c85fb";
}''', 'globals')
s=one(s,'''    g_chat_templates = common_chat_templates_init(g_model, "");
    g_sampler = new_sampler(nx_temp);
    return 0;''', '''    nx_template_present = false;
    nx_template_safe = false;
    nx_template_checked = false;
    nx_template_family = "desconhecido";
    nx_template_error = "nao_verificado";
    nx_template_origin = "fallback";
    // A escolha é exclusivamente do GGUF: nunca aplicar preset de outra família.
    const char * embedded = llama_model_chat_template(g_model, nullptr);
    const char * tool_use = llama_model_chat_template(g_model, "tool_use");
    nx_template_present = (embedded && *embedded) || (tool_use && *tool_use);
    if (nx_template_present) {
        nx_template_origin = "gguf";
        nx_template_family = nx_identify_template(embedded && *embedded ? embedded : tool_use);
    }
    try {
        g_chat_templates = common_chat_templates_init(g_model, "");
    } catch (const std::exception &) {
        nx_template_error = "inicializacao_template";
        g_chat_templates.reset();
    }
    // Sem template declarado: mantém compatibilidade legada SEM alegar template aplicado.
    if (nx_template_present && g_chat_templates) nx_validate_template();
    else { nx_template_checked = true; if (!nx_template_present) nx_template_error = "sem_template_gguf"; }
    g_sampler = new_sampler(nx_temp);
    return 0;''', 'prepare')
# For valid GGUF, Jinja is supported by engine. Explicit mode is only metadata (as upstream), but guard application.
# A mesma expressão aparece uma vez para system e outra para user; preservar cada guard.
src = '    const bool has_chat_template = common_chat_templates_was_explicit(g_chat_templates.get());'
if s.count(src) != 2: raise SystemExit('alpha22: esperava dois pontos de template upstream')
s=s.replace(src, '''    if (nx_template_present && (!nx_template_safe || !nx_template_system_supported)) {
        LOGe("Nexora: template incompatible with system role; request refused");
        env->ReleaseStringUTFChars(jsystem_prompt, system_prompt);
        return 7;
    }
    const bool has_chat_template = nx_template_present && nx_template_safe;''', 1)
s=s.replace(src, '''    if (nx_template_present && !nx_template_safe) {
        LOGe("Nexora: template GGUF incompatible; request refused");
        env->ReleaseStringUTFChars(juser_prompt, user_prompt);
        return 7;
    }
    const bool has_chat_template = nx_template_present && nx_template_safe;''', 1)
# Upstream format_single uses `use_jinja=false`, switch to Minja for model-specific templates.
s=one(s, 'new_msg, role == ROLE_USER, /* use_jinja */ false);', 'new_msg, role == ROLE_USER, /* use_jinja */ true);', 'jinja formatting')
# Tokenize already formatted output with parse_special=true and add_special=false; inject missing BOS once.
s=one(s,'''    const auto system_tokens = common_tokenize(g_context, formatted_system_prompt,
                                               has_chat_template, has_chat_template);''', '''    auto system_tokens = common_tokenize(g_context, formatted_system_prompt,
                                         !has_chat_template, has_chat_template);
    if (has_chat_template && !system_tokens.empty()) {
        const auto * vocab = llama_model_get_vocab(g_model);
        const llama_token bos = llama_vocab_bos(vocab);
        if (llama_vocab_get_add_bos(vocab) && bos != LLAMA_TOKEN_NULL && system_tokens[0] != bos)
            system_tokens.insert(system_tokens.begin(), bos);
    }''', 'system tokens')
s=one(s,'''    auto user_tokens = common_tokenize(g_context, formatted_user_prompt, has_chat_template, has_chat_template);''', '''    auto user_tokens = common_tokenize(g_context, formatted_user_prompt, false, has_chat_template);
    if (has_chat_template && current_position == 0 && !user_tokens.empty()) {
        const auto * vocab = llama_model_get_vocab(g_model);
        const llama_token bos = llama_vocab_bos(vocab);
        if (llama_vocab_get_add_bos(vocab) && bos != LLAMA_TOKEN_NULL && user_tokens[0] != bos)
            user_tokens.insert(user_tokens.begin(), bos);
    }''', 'user tokens')
# guard assistant handling before formatting if template not available; EOG path automatically skips without template
s=one(s,'''extern "C"
JNIEXPORT void JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_nativeResetConversation''', '''// Verificação somente em ModelReady, executada pelo dispatcher serializado.
extern "C"
JNIEXPORT jstring JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_nativeTemplateInfo(JNIEnv * env, jobject, jboolean verify) {
    if (verify && g_context && g_model) nx_validate_template();
    const std::string report = nx_template_report();
    return env->NewStringUTF(report.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_nativeResetConversation''', 'jni info')
s=one(s,'''    g_chat_templates.reset();
    g_batch = common_batch();''', '''    g_chat_templates.reset();
    nx_template_present = nx_template_safe = nx_template_checked = false;
    nx_template_system_supported = true;
    nx_template_error = "nao_verificado";
    nx_template_family = "desconhecido";
    nx_template_origin = "fallback";
    g_batch = common_batch();''','cleanup')
cpp.write_text(s)
s=api.read_text()
s=one(s, '    suspend fun resetConversation()', '''    suspend fun resetConversation()

    /** Somente template/tokenização de textos sintéticos, nunca gera resposta. */
    suspend fun templateReport(verify: Boolean = false): String''','api')
api.write_text(s)
s=impl.read_text()
s=one(s, '    private external fun nativeResetConversation()', '''    private external fun nativeResetConversation()

    @FastNative
    private external fun nativeTemplateInfo(verify: Boolean): String''','impl JNI')
s=one(s,'    override suspend fun resetConversation() = withContext(llamaDispatcher) {', '''    override suspend fun templateReport(verify: Boolean): String = withContext(llamaDispatcher) {
        check(_state.value is InferenceEngine.State.ModelReady) { "Motor não está pronto para verificar template" }
        nativeTemplateInfo(verify)
    }

    override suspend fun resetConversation() = withContext(llamaDispatcher) {''','impl API')
s=one(s, 'throw IOException("Failed to process user prompt: $result")',
    'throw IOException(if (result == 7) "Chat Template GGUF incompatível; consulte Compatibilidade dos modelos" else "Failed to process user prompt: $result")',
    'template error message')
s=one(s, 'RuntimeException("Failed to process system prompt: $result")',
    'RuntimeException(if (result == 7) "Papel system incompatível com este Chat Template GGUF" else "Failed to process system prompt: $result")',
    'system error message')
impl.write_text(s)
print('alpha22: modelo GGUF identificado, multirrodada verificada e proteção contra template incompatível')

#!/usr/bin/env python3
"""Nexora alpha.18: reforços verificáveis na revisão exata de llama.cpp.
Executar somente após bootstrap, patch_llama_metrics e patch_llama_preferences.
Falha se a revisão fixada mudar os trechos esperados; não faz substituição parcial.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
cpp = root / 'lib/src/main/cpp/ai_chat.cpp'
api = root / 'lib/src/main/java/com/arm/aichat/InferenceEngine.kt'
impl = root / 'lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt'

def replace_one(source, before, after, label):
    count = source.count(before)
    if count != 1:
        raise SystemExit(f'Nexora alpha.18: {label}: esperado 1 trecho, recebido {count}')
    return source.replace(before, after)

s = cpp.read_text()
# A revisão fixa registra toda mensagem formatada, inclusive trechos de PDFs.
s = replace_one(s,
    '    LOGi("%s: Formatted and added %s message: \\n%s\\n", __func__, role.c_str(), formatted.c_str());',
    '    LOGd("%s: %s message formatted (content omitted)", __func__, role.c_str());',
    'formatted message leak')
s = replace_one(s,
    '        LOGv("id: %d,\\tcached: `%s`,\\tnew: `%s`", new_token_id, cached_token_chars.c_str(), new_token_chars.c_str());',
    '        // Conteúdo de saída também é privado: não registrar token em Logcat.',
    'generated token leak')
# Controlar saída por tokens emitidos, nunca pelo deslocamento da posição KV.
s = replace_one(s, 'static llama_pos stop_generation_position;',
    '''static llama_pos stop_generation_position;
// Limite independente de context shifting: o KV pode retroceder, este contador não.
static int nx_predict_limit = 0;''', 'generation limit field')
s = replace_one(s,
    '''    stop_generation_position = 0;
    cached_token_chars.clear();''',
    '''    stop_generation_position = 0;
    nx_predict_limit = 0;
    cached_token_chars.clear();''', 'generation reset')
s = replace_one(s,
    '''    stop_generation_position = current_position + n_predict;
    return 0;''',
    '''    stop_generation_position = current_position + n_predict;
    nx_predict_limit = std::max(0, (int) n_predict);
    return 0;''', 'generation setup')
s = replace_one(s,
    '''    // Infinite text generation via context shifting
    if (current_position >= nx_context_size - OVERFLOW_HEADROOM) {
        LOGw("%s: Context full! Shifting...", __func__);
        shift_context();
    }

    // Stop if reaching the marked position
    if (current_position >= stop_generation_position) {
        LOGw("%s: STOP: hitting stop position: %d", __func__, stop_generation_position);
        nx_complete(2);
        return nullptr;
    }''',
    '''    // O número de tokens gerados não diminui quando a posição do KV recua.
    if (nx_generated_tokens >= nx_predict_limit) {
        LOGd("%s: STOP: generated %d of %d tokens", __func__, nx_generated_tokens, nx_predict_limit);
        nx_complete(2);
        return nullptr;
    }
    if (current_position >= nx_context_size - OVERFLOW_HEADROOM) {
        LOGw("%s: Context full! Shifting...", __func__);
        shift_context();
    }''', 'generation stop')
# Ajustar as posições dos batches durante o deslocamento e sincronizar posição global.
s = replace_one(s,
    '''    for (int i = 0; i < (int) tokens.size(); i += BATCH_SIZE) {
        const int cur_batch_size = std::min((int) tokens.size() - i, BATCH_SIZE);''',
    '''    llama_pos batch_position = start_pos;
    for (int i = 0; i < (int) tokens.size(); i += BATCH_SIZE) {
        const int cur_batch_size = std::min((int) tokens.size() - i, BATCH_SIZE);''', 'batch position initialization')
s = replace_one(s,
    '''        if (start_pos + i + cur_batch_size >= nx_context_size - OVERFLOW_HEADROOM) {
            LOGw("%s: Current batch won't fit into context! Shifting...", __func__);
            shift_context();
        }

        // Add tokens to the batch with proper positions
        for (int j = 0; j < cur_batch_size; j++) {
            const llama_token token_id = tokens[i + j];
            const llama_pos position = start_pos + i + j;''',
    '''        if (batch_position + cur_batch_size > nx_context_size - OVERFLOW_HEADROOM) {
            LOGw("%s: Current batch won't fit into context! Shifting...", __func__);
            shift_context();
            batch_position = current_position; // nunca reutilizar posição anterior ao shift
            if (batch_position + cur_batch_size > nx_context_size - OVERFLOW_HEADROOM) {
                LOGe("%s: Not enough KV space after shift", __func__);
                return 1;
            }
        }

        // Add tokens to the batch with proper positions
        for (int j = 0; j < cur_batch_size; j++) {
            const llama_token token_id = tokens[i + j];
            const llama_pos position = batch_position + j;''', 'stale batch positions')
s = replace_one(s,
    '''        if (decode_result) {
            LOGe("%s: llama_decode failed w/ %d", __func__, decode_result);
            return 1;
        }
    }
    return 0;''',
    '''        if (decode_result) {
            LOGe("%s: llama_decode failed w/ %d", __func__, decode_result);
            return 1;
        }
        batch_position += cur_batch_size;
        current_position = batch_position; // posição real dos tokens decodificados
    }
    return 0;''', 'batch accounting')
s = replace_one(s,
    '''    // Update position
    current_position += (int) user_tokens.size();
    stop_generation_position = current_position + n_predict;''',
    '''    // A rotina de batches já atualizou current_position, inclusive após shifts.
    stop_generation_position = current_position + n_predict;''', 'double count prompt')
# Dados nativos parciais devem ser liberados também após falhas de load/prepare.
s = replace_one(s,
    '''    reset_long_term_states();
    reset_short_term_states();

    // Free up resources
    common_sampler_free(g_sampler);
    g_chat_templates.reset();
    g_batch = common_batch();
    llama_free(g_context);
    llama_model_free(g_model);
}''',
    '''    if (g_context != nullptr) reset_long_term_states();
    else { chat_msgs.clear(); system_prompt_position = 0; current_position = 0; }
    reset_short_term_states();

    // Suporta recursos parcialmente inicializados após falha nativa.
    if (g_sampler != nullptr) common_sampler_free(g_sampler);
    g_sampler = nullptr;
    g_chat_templates.reset();
    g_batch = common_batch();
    if (g_context != nullptr) llama_free(g_context);
    g_context = nullptr;
    if (g_model != nullptr) llama_model_free(g_model);
    g_model = nullptr;
}''', 'partial resources cleanup')
# Reiniciar contexto por conversa preservando pesos GGUF na RAM.
s = replace_one(s,
    '''extern "C"
JNIEXPORT void JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_unload''',
    '''extern "C"
JNIEXPORT void JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_nativeResetConversation(JNIEnv *, jobject) {
    if (g_context == nullptr || g_sampler == nullptr) return;
    reset_long_term_states();
    reset_short_term_states();
    common_sampler_reset(g_sampler);
    nx_stop_reason = 0;
    nx_prompt_tokens = 0;
    nx_generated_tokens = 0;
    nx_prompt_started_us = 0;
    nx_prompt_elapsed_us = 0;
    nx_generation_started_us = 0;
    nx_generation_elapsed_us = 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_unload''', 'KV-only reset JNI')
cpp.write_text(s)

s = api.read_text()
s = replace_one(s, '    suspend fun setSystemPrompt(systemPrompt: String)',
    '''    suspend fun setSystemPrompt(systemPrompt: String)

    /** Limpa histórico/estado KV do chat sem descarregar pesos do modelo. */
    suspend fun resetConversation()''', 'interface reset')
api.write_text(s)

s = impl.read_text()
s = replace_one(s, '    private external fun nativeGenerationStats(): String',
    '''    private external fun nativeGenerationStats(): String

    @FastNative
    private external fun nativeResetConversation()''', 'Kotlin JNI reset')
s = replace_one(s,
    '''    override val lastGenerationStats: NativeGenerationStats? get() = latestStats''',
    '''    override val lastGenerationStats: NativeGenerationStats? get() = latestStats
    override suspend fun resetConversation() = withContext(llamaDispatcher) {
        check(_state.value is InferenceEngine.State.ModelReady) {
            "Cannot reset context in ${_state.value.javaClass.simpleName}"
        }
        nativeResetConversation()
        latestStats = null
        _cancelGeneration = false
        _readyForSystemPrompt = true
    }''', 'Kotlin reset API')
s = replace_one(s,
    '''                is InferenceEngine.State.Error -> {
                    Log.i(TAG, "Resetting error states...")
                    _state.value = InferenceEngine.State.Initialized''',
    '''                is InferenceEngine.State.Error -> {
                    Log.i(TAG, "Freeing partial native resources after failure...")
                    unload()
                    _readyForSystemPrompt = false
                    _state.value = InferenceEngine.State.Initialized''', 'Kotlin partial cleanup')
impl.write_text(s)
print('Nexora alpha.18: native privacy, token bounds, KV positions and context reset patches applied')

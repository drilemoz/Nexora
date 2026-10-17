#!/usr/bin/env python3
"""Patch estatisticas nativas da revisao fixa do llama.cpp. Falha se API mudar."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
cpp = root / 'lib/src/main/cpp/ai_chat.cpp'
impl = root / 'lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt'
api = root / 'lib/src/main/java/com/arm/aichat/InferenceEngine.kt'

def replace_one(s: str, before: str, after: str, file: str):
    times = s.count(before)
    if times != 1:
        raise SystemExit(f'Nexora alpha.13: patch {file} falhou ({times} ocorrencias): {before[:100]}')
    return s.replace(before, after)

s = cpp.read_text()
s = replace_one(s, 'static llama_model                      * g_model;', '''// Estatisticas pertencem exclusivamente ao dispatcher JNI serializado.
// Reason: 0=desconhecido, 1=fim natural (EOG), 2=limite, 3=falha de decode.
static int nx_stop_reason = 0;
static int nx_prompt_tokens = 0;
static int nx_generated_tokens = 0;
static int64_t nx_prompt_started_us = 0;
static int64_t nx_prompt_elapsed_us = 0;
static int64_t nx_generation_started_us = 0;
static int64_t nx_generation_elapsed_us = 0;

static void nx_complete(int reason) {
    nx_stop_reason = reason;
    nx_generation_elapsed_us = nx_generation_started_us > 0
        ? ggml_time_us() - nx_generation_started_us : 0;
}

static llama_model                      * g_model;''', 'cpp globals')
s = replace_one(s, '    // Obtain and tokenize user prompt\n', '''    nx_stop_reason = 0;
    nx_prompt_tokens = 0;
    nx_generated_tokens = 0;
    nx_prompt_elapsed_us = 0;
    nx_generation_elapsed_us = 0;
    nx_generation_started_us = 0;
    nx_prompt_started_us = ggml_time_us();

    // Obtain and tokenize user prompt
''', 'cpp reset')
s = replace_one(s, '''    if (decode_tokens_in_batches(g_context, g_batch, user_tokens, current_position, true)) {
        LOGe("%s: llama_decode() failed!", __func__);
        return 2;
    }

    // Update position
    current_position += (int) user_tokens.size();
    stop_generation_position = current_position + n_predict;''', '''    if (decode_tokens_in_batches(g_context, g_batch, user_tokens, current_position, true)) {
        LOGe("%s: llama_decode() failed!", __func__);
        nx_stop_reason = 3;
        return 2;
    }

    // Contexto: contabilizar somente tokens que realmente entraram após truncamento.
    nx_prompt_tokens = (int) user_tokens.size();
    nx_prompt_elapsed_us = ggml_time_us() - nx_prompt_started_us;
    nx_generation_started_us = ggml_time_us();

    // Update position
    current_position += (int) user_tokens.size();
    stop_generation_position = current_position + n_predict;''', 'cpp prompt')
s = replace_one(s, '''        LOGw("%s: STOP: hitting stop position: %d", __func__, stop_generation_position);
        return nullptr;''','''        LOGw("%s: STOP: hitting stop position: %d", __func__, stop_generation_position);
        nx_complete(2);
        return nullptr;''','cpp limit')
s = replace_one(s, '''        LOGe("%s: llama_decode() failed for generated token", __func__);
        return nullptr;''','''        LOGe("%s: llama_decode() failed for generated token", __func__);
        nx_complete(3);
        return nullptr;''','cpp decode')
s = replace_one(s, '''    // Update position
    current_position++;

    // Stop if next token is EOG''','''    // Update position
    current_position++;
    nx_generated_tokens++;

    // Stop if next token is EOG''','cpp tokens')
s = replace_one(s, '''        chat_add_and_format(ROLE_ASSISTANT, assistant_ss.str());
        return nullptr;''','''        chat_add_and_format(ROLE_ASSISTANT, assistant_ss.str());
        nx_complete(1);
        return nullptr;''','cpp eos')
s = replace_one(s, '''extern "C"
JNIEXPORT void JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_unload''', '''// Estatisticas sem texto privado, disponiveis apos a execucao serializada.
extern "C"
JNIEXPORT jstring JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_nativeGenerationStats(
        JNIEnv *env, jobject /*unused*/) {
    std::string value = std::to_string(nx_stop_reason) + "," +
        std::to_string(nx_prompt_tokens) + "," +
        std::to_string(nx_generated_tokens) + "," +
        std::to_string(nx_prompt_elapsed_us) + "," +
        std::to_string(nx_generation_elapsed_us);
    return env->NewStringUTF(value.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_unload''', 'cpp JNI')
cpp.write_text(s)

s = api.read_text()
s = replace_one(s, '    val state: StateFlow<State>', '    val state: StateFlow<State>\n\n    /** Ultimo termino nativo da geracao; nulo antes do termino/cancelamento. */\n    val lastGenerationStats: NativeGenerationStats?', 'api stats')
s += '''
/** Contagem de tokens nativos, nao fragmentos UTF-8. Tempos em milissegundos. */
data class NativeGenerationStats(
    val reason: Int,
    val promptTokens: Int,
    val generatedTokens: Int,
    val promptMs: Long,
    val generationMs: Long
) {
    val tokensPerSecond: Double?
        get() = if (generationMs > 0L && generatedTokens > 0)
            generatedTokens * 1000.0 / generationMs else null
}
'''
api.write_text(s)

s = impl.read_text()
s = replace_one(s, 'import com.arm.aichat.InferenceEngine\n','import com.arm.aichat.InferenceEngine\nimport com.arm.aichat.NativeGenerationStats\n', 'impl import')
s = replace_one(s, '''    @FastNative
    private external fun generateNextToken(): String?''', '''    @FastNative
    private external fun generateNextToken(): String?

    @FastNative
    private external fun nativeGenerationStats(): String''', 'impl JNI')
s = replace_one(s, '''    override val state: StateFlow<InferenceEngine.State> = _state.asStateFlow()''','''    override val state: StateFlow<InferenceEngine.State> = _state.asStateFlow()
    @Volatile private var latestStats: NativeGenerationStats? = null
    override val lastGenerationStats: NativeGenerationStats? get() = latestStats''', 'impl state')
s = replace_one(s, '''        try {
            Log.i(TAG, "Sending user prompt...")''', '''        try {
            latestStats = null
            Log.i(TAG, "Sending user prompt...")''', 'impl reset')
s = replace_one(s, '''                    Log.e(TAG, "Failed to process user prompt: $result")
                    return@flow''', '''                    throw IOException("Failed to process user prompt: $result")''', 'impl error')
s = replace_one(s, '''            if (_cancelGeneration) {
                Log.i(TAG, "Assistant generation aborted per requested.")
            } else {
                Log.i(TAG, "Assistant generation complete. Awaiting user prompt...")
            }
            _state.value = InferenceEngine.State.ModelReady''', '''            if (_cancelGeneration) {
                Log.i(TAG, "Assistant generation aborted per requested.")
            } else {
                val fields = nativeGenerationStats().split(',')
                latestStats = if (fields.size == 5) runCatching {
                    NativeGenerationStats(fields[0].toInt(), fields[1].toInt(), fields[2].toInt(),
                        fields[3].toLong() / 1000, fields[4].toLong() / 1000)
                }.getOrNull() else null
                Log.i(TAG, "Assistant generation ended; native reason=${latestStats?.reason}")
            }
            _state.value = InferenceEngine.State.ModelReady''', 'impl finish')
s = replace_one(s, '''        } catch (e: CancellationException) {
            Log.i(TAG, "Assistant generation's flow collection cancelled.")''', '''        } catch (e: CancellationException) {
            latestStats = null
            Log.i(TAG, "Assistant generation's flow collection cancelled.")''', 'impl cancellation')
s = replace_one(s, '''        } catch (e: Exception) {
            Log.e(TAG, "Error during generation!", e)''', '''        } catch (e: Exception) {
            latestStats = null
            Log.e(TAG, "Error during generation!", e)''', 'impl fault')
impl.write_text(s)
print('Nexora alpha.13: patch nativo + contrato Kotlin aplicado')

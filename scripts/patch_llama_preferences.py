#!/usr/bin/env python3
# Extensão idempotente da revisão pinned após métricas
from pathlib import Path
import sys
root=Path(sys.argv[1]); cpp=root/'lib/src/main/cpp/ai_chat.cpp'; api=root/'lib/src/main/java/com/arm/aichat/InferenceEngine.kt'; impl=root/'lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt'
def once(p,needle,replacement):
 s=p.read_text();n=s.count(needle)
 if n!=1: raise SystemExit(f'patch prefs failed {p.name}: {n} hits for {needle[:70]}')
 p.write_text(s.replace(needle,replacement))
once(cpp,'static llama_model                      * g_model;','''// Configuração recebida antes de loadModel, protegida pelo dispatcher Kotlin.
static int nx_context_size = 4096;
static int nx_threads = 4;
static float nx_temp = 0.3f;
static float nx_top_p = 0.95f;
static int nx_top_k = 40;
static float nx_min_p = 0.05f;
static float nx_repeat = 1.0f;
extern "C" JNIEXPORT void JNICALL
Java_com_arm_aichat_internal_InferenceEngineImpl_nativeConfigureModel(
        JNIEnv *, jobject, jint contextSize, jint threads, jfloat temp, jfloat topP, jint topK, jfloat minP, jfloat repeat) {
    nx_context_size = (contextSize == 2048 || contextSize == 4096 || contextSize == 8192) ? contextSize : 4096;
    nx_threads = std::max(2, std::min(4, (int) threads));
    nx_temp = std::max(0.0f, std::min(1.5f, (float)temp));
    nx_top_p = std::max(0.05f, std::min(1.0f, (float)topP));
    nx_top_k = std::max(1, std::min(100, (int)topK));
    nx_min_p = std::max(0.0f, std::min(1.0f, (float)minP));
    nx_repeat = std::max(1.0f, std::min(1.5f, (float)repeat));
}
static llama_model                      * g_model;''')
once(cpp,'''    const int n_threads = std::max(N_THREADS_MIN, std::min(N_THREADS_MAX,
                                                     (int) sysconf(_SC_NPROCESSORS_ONLN) -
                                                     N_THREADS_HEADROOM));''','''    const int n_threads = nx_threads;''')
once(cpp,'''    sparams.temp = temp;
    return common_sampler_init(g_model, sparams);''','''    sparams.temp = temp;
    sparams.top_p = nx_top_p;
    sparams.top_k = nx_top_k;
    sparams.min_p = nx_min_p;
    sparams.penalty_repeat = nx_repeat;
    return common_sampler_init(g_model, sparams);''')
once(cpp,'auto *context = init_context(g_model);','auto *context = init_context(g_model, nx_context_size);')
once(cpp,'g_sampler = new_sampler(DEFAULT_SAMPLER_TEMP);','g_sampler = new_sampler(nx_temp);')
# runtime window must match chosen n_ctx in prompt truncation and context shifting
s=cpp.read_text();n=s.count('DEFAULT_CONTEXT_SIZE - OVERFLOW_HEADROOM')
if n<2: raise SystemExit(f'expected >=2 runtime context bounds, got {n}')
cpp.write_text(s.replace('DEFAULT_CONTEXT_SIZE - OVERFLOW_HEADROOM','nx_context_size - OVERFLOW_HEADROOM'))
once(api,'    val lastGenerationStats: NativeGenerationStats?','''    val lastGenerationStats: NativeGenerationStats?
    /** Ajuste do motor antes do próximo loadModel, não altera sessão em andamento. */
    suspend fun configureModel(contextSize:Int, threads:Int, temp:Float, topP:Float, topK:Int, minP:Float, repeatPenalty:Float)''')
once(impl,'''    private external fun nativeGenerationStats(): String''','''    private external fun nativeGenerationStats(): String

    @FastNative
    private external fun nativeConfigureModel(contextSize:Int, threads:Int, temp:Float, topP:Float, topK:Int, minP:Float, repeatPenalty:Float)''')
once(impl,'''    override val lastGenerationStats: NativeGenerationStats? get() = latestStats''','''    override val lastGenerationStats: NativeGenerationStats? get() = latestStats
    override suspend fun configureModel(contextSize:Int, threads:Int, temp:Float, topP:Float, topK:Int, minP:Float, repeatPenalty:Float) = withContext(llamaDispatcher) {
        check(_state.value is InferenceEngine.State.Initialized || _state.value is InferenceEngine.State.Uninitialized || _state.value is InferenceEngine.State.ModelReady) { "Não alterar ajustes durante inferência" }
        nativeConfigureModel(contextSize, threads, temp, topP, topK, minP, repeatPenalty)
    }''')
print('Nexora alpha.16: preferências reais e API JNI aplicadas')

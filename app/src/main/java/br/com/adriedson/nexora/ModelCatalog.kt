package br.com.adriedson.nexora

/** Lista selecionada manualmente, revisada em 17/10/2026. Não é ranking dinâmico. */
data class CatalogModel(
    val id: String,
    val label: String,
    val description: String,
    val filename: String,
    val sizeLabel: String,
    val approximateBytes: Long,
    val source: String,
    val sha256: String,
) {
    val downloadUrl: String get() = "$source/resolve/$revision/$filename"
    val pageUrl: String get() = "$source/blob/$revision/$filename"
    private val revision: String get() = ModelCatalog.revisionFor(id)
}

object ModelCatalog {
    private val revisions = mapOf(
        "smollm2-360m" to "ab63027800638cfa050c6e9dc03428cc02934d3d",
        "qwen3-06b" to "9e43cb1438c9523e11af66d6b4b2a424a1aa0e39",
        "qwen3-17b" to "136153a4f0744c0fa07aa2807d15c4bc6dd28709",
        "qwen3-4b" to "main",
    )
    internal fun revisionFor(id: String): String = revisions.getValue(id)
    val models: List<CatalogModel> = listOf(
        CatalogModel(
            "smollm2-360m", "SmolLM2 360M • Q4_K_M",
            "Muito leve para testar. Treinado principalmente para inglês; português limitado.",
            "SmolLM2-360M-Instruct-Q4_K_M.gguf", "271 MB", 271_000_000L,
            "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF",
            "2fa3f013dcdd7b99f9b237717fa0b12d75bbb89984cc1274be1471a465bac9c2",
        ),
        CatalogModel(
            "qwen3-06b", "Qwen3 0.6B • Q4_K_M",
            "Recomendado para começar: pequeno, multilíngue, bom equilíbrio para ARM64.",
            "Qwen_Qwen3-0.6B-Q4_K_M.gguf", "484 MB", 484_000_000L,
            "https://huggingface.co/bartowski/Qwen_Qwen3-0.6B-GGUF",
            "9acfc1e001311f34b4252001b626f2e466d592a42065f66571bff3790d4e1b14",
        ),
        CatalogModel(
            "qwen3-17b", "Qwen3 1.7B • Q4_K_M",
            "Mais exigente. Pode faltar RAM no celular; o download não garante que irá carregar.",
            "Qwen_Qwen3-1.7B-Q4_K_M.gguf", "1,28 GB", 1_280_000_000L,
            "https://huggingface.co/bartowski/Qwen_Qwen3-1.7B-GGUF",
            "72c5c3cb38fa32d5256e2fe30d03e7a64c6c79e668ad84057e3bd66e250b24fb",
        ),
        CatalogModel(
            "qwen3-4b", "Qwen3 4B • Q4_K_M",
            "Experimental: exige mais RAM que o 1.7B. Pode fechar por falta de memória ou ficar lento; teste com outros apps fechados.",
            "Qwen3-4B-Q4_K_M.gguf", "2,5 GB", 2_500_000_000L,
            "https://huggingface.co/Qwen/Qwen3-4B-GGUF",
            "7485fe6f11af29433bc51cab58009521f205840f5b4ae3a32fa7f92e8534fdf5",
        ),
    )
    fun byId(id: String?): CatalogModel? = models.firstOrNull { it.id == id }
}

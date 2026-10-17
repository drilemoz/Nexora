package br.com.adriedson.nexora

/** Validacoes puras para testes locais sem Android SDK. */
object ModelCatalogChecks {
    fun isValidEntry(model: CatalogModel): Boolean =
        model.id.matches(Regex("[a-z0-9-]+")) &&
        ModelFiles.safeName(model.filename) == model.filename &&
        model.source.startsWith("https://huggingface.co/") &&
        model.downloadUrl.contains("/resolve/") &&
        model.pageUrl.contains("/blob/") &&
        model.sha256.matches(Regex("[a-f0-9]{64}")) &&
        model.approximateBytes > 1_000_000L
}

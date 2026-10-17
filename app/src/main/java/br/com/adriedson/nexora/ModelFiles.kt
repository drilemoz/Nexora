package br.com.adriedson.nexora

/** No paths from a document provider are ever trusted for filesystem writes. */
object ModelFiles {
    fun safeName(source: String): String {
        val plain = source.substringAfterLast('/').substringAfterLast('\\')
        val normalized = plain.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_')
        require(normalized.endsWith(".gguf", ignoreCase = true)) { "Selecione um arquivo .gguf" }
        require(normalized.length in 6..128) { "Nome de arquivo inválido" }
        return normalized
    }
    fun hasGgufMagic(header: ByteArray): Boolean =
        header.size >= 4 && header[0] == 0x47.toByte() && header[1] == 0x47.toByte() &&
        header[2] == 0x55.toByte() && header[3] == 0x46.toByte()
}

package br.com.adriedson.nexora

/** Texto visível apenas: nunca alterar o nome/path real exigido pela engine. */
object ModelDisplayNames {
    fun defaultName(original: String): String {
        val filename = original.substringAfterLast('/').substringAfterLast('\\')
        if (!filename.endsWith(".gguf", ignoreCase = true)) return original
        val stem = filename.dropLast(5)
        val qwen = Regex("^(?:Qwen[_-])?Qwen3[-_ ]?(\\d+(?:\\.\\d+)?)B(?:[-_].*)?$", RegexOption.IGNORE_CASE).matchEntire(stem)
        if (qwen != null) return "Qwen 3 • ${qwen.groupValues[1]}B"
        val compact = stem.replace(Regex("[-_]Q[0-9]+(?:_[A-Z0-9]+)*$", RegexOption.IGNORE_CASE), "")
            .replace('_', ' ').replace('-', ' ').trim()
        return compact.ifBlank { stem }
    }

    fun validCustomName(candidate: String): String? {
        val trimmed = candidate.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty() || trimmed.length > 48 || trimmed.any { Character.isISOControl(it) }) return null
        return trimmed
    }
}

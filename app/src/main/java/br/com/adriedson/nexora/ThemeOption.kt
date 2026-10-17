package br.com.adriedson.nexora

/** Opcoes persistidas como chaves estaveis, independentes da lingua da interface. */
enum class ThemeOption(val key: String, val title: String) {
    SYSTEM("system", "Padrão do sistema"),
    LIGHT("light", "Claro (branco)"),
    DARK("dark", "Escuro"),
    AMOLED("amoled", "Preto AMOLED");

    companion object {
        fun fromKey(key: String?): ThemeOption = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

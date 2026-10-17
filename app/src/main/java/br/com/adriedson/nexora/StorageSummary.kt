package br.com.adriedson.nexora

import android.os.StatFs
import java.io.File
import java.util.Locale

object StorageSummary {
    data class Space(val free: Long, val total: Long)
    fun read(folder: File): Space {
        val st = StatFs(folder.absolutePath)
        return Space(st.availableBytes, st.totalBytes)
    }
    fun size(bytes: Long): String {
        val gb = bytes.toDouble() / 1_000_000_000.0
        return if (gb >= 1.0) String.format(Locale("pt", "BR"), "%.1f GB", gb)
        else String.format(Locale("pt", "BR"), "%.0f MB", bytes.toDouble() / 1_000_000.0)
    }
}

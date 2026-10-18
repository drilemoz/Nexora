package br.com.adriedson.nexora

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Datas calculadas no fuso atual: hoje muda automaticamente após a meia-noite. */
object ConversationDateGroups {
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

    fun dateOf(timestampMs: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate()

    fun section(updatedMs: Long, pinned: Boolean, today: LocalDate = LocalDate.now(),
                zone: ZoneId = ZoneId.systemDefault()): String {
        if (pinned) return "FIXADAS"
        val day = dateOf(updatedMs, zone)
        return if (day == today) "HOJE" else day.format(dateFormat)
    }

    fun pinnedDetail(updatedMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dateOf(updatedMs, zone).format(dateFormat)
}

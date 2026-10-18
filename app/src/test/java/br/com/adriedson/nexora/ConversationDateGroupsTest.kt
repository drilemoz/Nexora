package br.com.adriedson.nexora

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ConversationDateGroupsTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 10, 18)
    private fun timestamp(day: LocalDate): Long = day.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test fun todayUsesToday() = assertEquals("HOJE", ConversationDateGroups.section(timestamp(today), false, today, zone))
    @Test fun yesterdayUsesExactDate() = assertEquals("17/10/2026", ConversationDateGroups.section(timestamp(today.minusDays(1)), false, today, zone))
    @Test fun oldConversationsKeepOwnDays() {
        assertEquals("05/10/2026", ConversationDateGroups.section(timestamp(LocalDate.of(2026,10,5)), false, today, zone))
        assertEquals("04/10/2026", ConversationDateGroups.section(timestamp(LocalDate.of(2026,10,4)), false, today, zone))
    }
    @Test fun crossingYearKeepsExactDate() = assertEquals("31/12/2025",
        ConversationDateGroups.section(timestamp(LocalDate.of(2025,12,31)), false, LocalDate.of(2026,1,1), zone))
    @Test fun futureDateNotCalledToday() = assertEquals("20/10/2026",
        ConversationDateGroups.section(timestamp(today.plusDays(2)), false, today, zone))
    @Test fun pinnedHeaderAndDateDetail() {
        assertEquals("FIXADAS", ConversationDateGroups.section(timestamp(today.minusDays(9)), true, today, zone))
        assertEquals("09/10/2026", ConversationDateGroups.pinnedDetail(timestamp(today.minusDays(9)), zone))
    }
    @Test fun localTimezoneChangesTheCalendarDay() {
        val millis = ZonedDateTime.of(2026,10,18,1,0,0,0,ZoneId.of("UTC")).toInstant().toEpochMilli()
        assertEquals(LocalDate.of(2026,10,17), ConversationDateGroups.dateOf(millis,zone))
        assertEquals(LocalDate.of(2026,10,18), ConversationDateGroups.dateOf(millis,ZoneId.of("UTC")))
    }
}

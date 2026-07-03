package com.example.smartcalendar.agent.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateParamResolverTest {
    private val resolver = DateParamResolver()
    private val currentDate = LocalDate.of(2026, 6, 29)

    @Test
    fun `hom nay resolves to a single-day range`() {
        assertEquals(
            currentDate to currentDate,
            resolver.resolveDateRange("hom nay co gi", currentDate)
        )
    }

    @Test
    fun `ngay mai resolves to a single-day range`() {
        val tomorrow = LocalDate.of(2026, 6, 30)

        assertEquals(
            tomorrow to tomorrow,
            resolver.resolveDateRange("mai co gi", currentDate)
        )
    }

    @Test
    fun `single explicit date resolves to a single-day range`() {
        val date = LocalDate.of(2026, 7, 1)

        assertEquals(
            date to date,
            resolver.resolveDateRange("01/07 co mon nao", currentDate)
        )
    }

    @Test
    fun `two explicit dates resolve to an ordered range`() {
        assertEquals(
            LocalDate.of(2026, 6, 20) to LocalDate.of(2026, 6, 30),
            resolver.resolveDateRange("30/06 toi 20/06 co gi", currentDate)
        )
    }
}

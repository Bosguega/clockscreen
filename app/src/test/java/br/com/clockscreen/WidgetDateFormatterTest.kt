package br.com.clockscreen

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WidgetDateFormatterTest {

    @Test
    fun `weekday is formatted in Brazilian Portuguese`() {
        val date = calendarDate(2026, Calendar.JUNE, 3)
        assertEquals("Quarta-feira", WidgetDateFormatter.weekday(date))
    }

    @Test
    fun `date is formatted in Brazilian Portuguese`() {
        val date = calendarDate(2026, Calendar.JUNE, 3)
        assertEquals("03 de junho de 2026", WidgetDateFormatter.date(date))
    }

    private fun calendarDate(year: Int, month: Int, day: Int) =
        Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.time
}

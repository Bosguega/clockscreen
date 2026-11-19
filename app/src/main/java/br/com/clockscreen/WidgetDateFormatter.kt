package br.com.clockscreen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatação das datas exibidas no widget.
 * O locale é fixado em português do Brasil, independentemente do idioma do aparelho.
 */
internal object WidgetDateFormatter {

    private val brazilianPortuguese = Locale("pt", "BR")

    fun weekday(date: Date = Date()): String =
        SimpleDateFormat("EEEE", brazilianPortuguese).format(date).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(brazilianPortuguese) else it.toString()
        }

    fun date(date: Date = Date()): String =
        SimpleDateFormat("dd 'de' MMMM 'de' yyyy", brazilianPortuguese).format(date)
}

package br.com.clockscreen

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class ClockWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_DATE = "br.com.clockscreen.ACTION_UPDATE_DATE"

        fun updateDate(context: Context) {
            // Removed isDaemon=true so the thread isn't killed prematurely (#10)
            thread(start = true) {
                val weatherData = WeatherService.getWeather(context)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, ClockWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                val brLocale = Locale("pt", "BR")

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_clock)

                    // Aplica background baseado no clima
                    if (weatherData != null) {
                        val bgResource = WeatherBackgroundMapper.getBackgroundResource(
                            weatherData.weatherCode,
                            weatherData.isDaytime
                        )
                        views.setInt(
                            R.id.widget_background_container,
                            "setBackgroundResource",
                            bgResource
                        )

                        // Ajusta cores do texto
                        val textColorPrimary = WeatherBackgroundMapper.getTextColorPrimary(
                            weatherData.isDaytime,
                            weatherData.weatherCode
                        )
                        val textColorSecondary = WeatherBackgroundMapper.getTextColorSecondary(
                            weatherData.isDaytime,
                            weatherData.weatherCode
                        )
                        views.setTextColor(
                            R.id.tv_widget_weekday,
                            Color.parseColor(textColorPrimary)
                        )
                        views.setTextColor(
                            R.id.tv_widget_date,
                            Color.parseColor(textColorSecondary)
                        )

                        // Mostra temperatura
                        val tempText = "${weatherData.temperature.toInt()}°C"
                        views.setTextViewText(R.id.tv_widget_temperature, tempText)

                        // Mostra ícone do clima
                        views.setTextViewText(
                            R.id.tv_widget_weather_icon,
                            WeatherBackgroundMapper.getWeatherEmoji(weatherData.weatherCode, weatherData.isDaytime)
                        )

                        // Mostra cidade
                        val cityName = weatherData.cityName
                            ?: context.getString(R.string.location_unavailable)
                        views.setTextViewText(R.id.tv_widget_city, cityName)
                    }

                    // Data no fuso horário local
                    val calendar = Calendar.getInstance(TimeZone.getDefault(), brLocale)

                    // Dia da semana (ex: "Quarta-feira,")
                    val weekdayFormat = SimpleDateFormat("EEEE,", brLocale)
                    val weekdayText = weekdayFormat.format(calendar.time).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(brLocale) else it.toString()
                    }
                    views.setTextViewText(R.id.tv_widget_weekday, weekdayText)

                    // Data completa (ex: "08 de Abril de 2026")
                    val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", brLocale)
                    val dateText = dateFormat.format(calendar.time).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(brLocale) else it.toString()
                    }
                    views.setTextViewText(R.id.tv_widget_date, dateText)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }

                // Reagenda para a próxima meia-noite (só se tiver permissão)
                if (PermissionUtils.hasAlarmPermission(context)) {
                    DateAlarmScheduler.scheduleMidnightUpdate(context)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Atualiza todos os widgets
        updateDate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE_DATE -> updateDate(context)
            Intent.ACTION_TIMEZONE_CHANGED -> updateDate(context)
        }
    }
}

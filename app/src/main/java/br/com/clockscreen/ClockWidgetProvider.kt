package br.com.clockscreen

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClockWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_DATE = "br.com.clockscreen.ACTION_UPDATE_DATE"

        fun updateDate(context: Context, pendingResult: android.content.BroadcastReceiver.PendingResult? = null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ClockWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val weatherData = WeatherService.getWeather(context)
                    val brLocale = Locale("pt", "BR")

                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_clock)

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

                            val textColorPrimary = WeatherBackgroundMapper.getTextColorPrimary(
                                weatherData.isDaytime,
                                weatherData.weatherCode
                            )
                            val textColorSecondary = WeatherBackgroundMapper.getTextColorSecondary(
                                weatherData.isDaytime,
                                weatherData.weatherCode
                            )
                            views.setTextColor(R.id.tv_widget_weekday, Color.parseColor(textColorPrimary))
                            views.setTextColor(R.id.tv_widget_date, Color.parseColor(textColorSecondary))

                            views.setTextViewText(R.id.tv_widget_temperature, "${weatherData.temperature.toInt()}°C")
                            views.setTextViewText(
                                R.id.tv_widget_weather_icon,
                                WeatherBackgroundMapper.getWeatherEmoji(weatherData.weatherCode, weatherData.isDaytime)
                            )
                            val cityName = weatherData.cityName ?: context.getString(R.string.location_unavailable)
                            views.setTextViewText(R.id.tv_widget_city, cityName)
                        }

                        val calendar = Calendar.getInstance(TimeZone.getDefault(), brLocale)

                        val weekdayFormat = SimpleDateFormat("EEEE,", brLocale)
                        val weekdayText = weekdayFormat.format(calendar.time).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(brLocale) else it.toString()
                        }
                        views.setTextViewText(R.id.tv_widget_weekday, weekdayText)

                        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", brLocale)
                        val dateText = dateFormat.format(calendar.time).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(brLocale) else it.toString()
                        }
                        views.setTextViewText(R.id.tv_widget_date, dateText)

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }

                    if (PermissionUtils.hasAlarmPermission(context)) {
                        DateAlarmScheduler.scheduleMidnightUpdate(context)
                    }
                } finally {
                    // Libera o BroadcastReceiver para que o processo não seja encerrado prematuramente
                    pendingResult?.finish()
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateDate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE_DATE, Intent.ACTION_TIMEZONE_CHANGED -> {
                val pendingResult = goAsync()
                updateDate(context, pendingResult)
            }
        }
    }
}

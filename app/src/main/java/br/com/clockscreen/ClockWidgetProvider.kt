package br.com.clockscreen

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClockWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "ClockWidget"
        const val ACTION_UPDATE_DATE = "br.com.clockscreen.ACTION_UPDATE_DATE"

        /**
         * Atualiza todos os widgets com dados do clima e data.
         * Usa WeatherCache para evitar chamadas de rede desnecessárias.
         */
        fun updateDate(context: Context, pendingResult: android.content.BroadcastReceiver.PendingResult? = null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ClockWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "Nenhum widget para atualizar")
                pendingResult?.finish()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Tenta usar cache primeiro (mais rápido, evita rede)
                    val cache = WeatherCache(context)
                    var weatherData = if (cache.isCacheValid()) {
                        val cached = cache.getCachedWeather()
                        if (cached.apiResponse != null) {
                            try {
                                WeatherService.getWeather(context)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    } else null

                    // Se cache não é válido, busca da API
                    if (weatherData == null) {
                        weatherData = WeatherService.getWeather(context)
                    }

                    val brLocale = Locale("pt", "BR")

                    for (appWidgetId in appWidgetIds) {
                        // Cria um novo RemoteViews para cada widget
                        val views = RemoteViews(context.packageName, R.layout.widget_clock)

                        if (weatherData != null) {
                            // Background dinâmico baseado no clima e horário
                            val bgResource = WeatherBackgroundMapper.getBackgroundResource(
                                weatherData.weatherCode,
                                weatherData.isDaytime
                            )
                            views.setInt(
                                R.id.widget_background_container,
                                "setBackgroundResource",
                                bgResource
                            )

                            // Cores de texto baseadas no background
                            val textColorPrimary = WeatherBackgroundMapper.getTextColorPrimary(
                                weatherData.isDaytime,
                                weatherData.weatherCode
                            )
                            val textColorSecondary = WeatherBackgroundMapper.getTextColorSecondary(
                                weatherData.isDaytime,
                                weatherData.weatherCode
                            )

                            // Hora (TextClock atualiza sozinho)
                            // Dados do clima
                            views.setTextViewText(
                                R.id.tv_widget_temperature,
                                "${weatherData.temperature.toInt()}°C"
                            )
                            views.setTextViewText(
                                R.id.tv_widget_weather_icon,
                                WeatherBackgroundMapper.getWeatherEmoji(
                                    weatherData.weatherCode,
                                    weatherData.isDaytime
                                )
                            )
                            val cityName = weatherData.cityName
                                ?: context.getString(R.string.location_unavailable)
                            views.setTextViewText(R.id.tv_widget_city, cityName)

                            // Cores do texto
                            views.setTextColor(
                                R.id.tv_widget_weekday,
                                Color.parseColor(textColorPrimary)
                            )
                            views.setTextColor(
                                R.id.tv_widget_date,
                                Color.parseColor(textColorSecondary)
                            )
                        }

                        // Data e dia da semana (sempre atualiza)
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

                    // Reagenda o alarme periódico para manter o widget atualizado
                    if (PermissionUtils.hasAlarmPermission(context)) {
                        DateAlarmScheduler.schedulePeriodicUpdate(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao atualizar widget: ${e.message}", e)
                } finally {
                    // Libera o BroadcastReceiver para que o processo não seja encerrado prematuramente
                    pendingResult?.finish()
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate: ${appWidgetIds.size} widget(s)")
        updateDate(context)
    }

    override fun onEnabled(context: Context) {
        // Chamado quando o primeiro widget é adicionado
        super.onEnabled(context)
        Log.d(TAG, "onEnabled: primeiro widget adicionado")
        if (PermissionUtils.hasAlarmPermission(context)) {
            DateAlarmScheduler.schedulePeriodicUpdate(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        // Chamado quando o widget é redimensionado
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d(TAG, "onAppWidgetOptionsChanged: widget $appWidgetId redimensionado")
        updateDate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE_DATE, Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d(TAG, "onReceive: ${intent.action}")
                val pendingResult = goAsync()
                updateDate(context, pendingResult)
            }
        }
    }
}

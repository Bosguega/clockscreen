package br.com.clockscreen

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WeatherService {

    private const val TAG = "WeatherService"
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 min
    private const val HTTP_TIMEOUT_MS = 10_000 // 10s

    fun getWeather(context: Context): WeatherData? {
        return getForecast(context)?.current
    }

    fun getForecast(context: Context): WeatherForecast? {
        val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
        val cacheTime = prefs.getLong("cache_time", 0)
        val currentTime = System.currentTimeMillis()

        if (currentTime - cacheTime < CACHE_DURATION_MS) {
            Log.d(TAG, "📦 Usando dados do cache")
            val cachedResponse = prefs.getString("api_response", null)
            val cachedCity = prefs.getString("city_name", null)
            if (cachedResponse != null) {
                return try {
                    parseApiResponse(JSONObject(cachedResponse), cachedCity)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao ler cache: ${e.message}", e)
                    null
                }
            }
        }

        return try {
            val location = getLastKnownLocation(context)

            if (location == null) {
                Log.w(TAG, "⚠️ Sem localização, usando fallback")
                fetchWeather(
                    lat = -23.55,
                    lon = -46.63,
                    cityName = context.getString(R.string.fallback_city),
                    context = context
                )
            } else {
                Log.d(
                    TAG,
                    "📍 lat=${location.latitude}, lon=${location.longitude}"
                )

                val cityName = getCityName(
                    context,
                    location.latitude,
                    location.longitude
                )

                fetchWeather(
                    lat = location.latitude,
                    lon = location.longitude,
                    cityName = cityName,
                    context = context
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao obter clima: ${e.message}", e)
            getDefaultForecast()
        }
    }

    private fun fetchWeather(
        lat: Double,
        lon: Double,
        cityName: String?,
        context: Context
    ): WeatherForecast? {
        return try {
            val apiUrl = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&forecast_days=7"

            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = HTTP_TIMEOUT_MS
            connection.readTimeout = HTTP_TIMEOUT_MS
            connection.requestMethod = "GET"

            val response = try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw java.io.IOException("HTTP $responseCode")
                }
                connection.inputStream.bufferedReader().readText()
            } finally {
                connection.disconnect()
            }

            Log.d(TAG, "🌤️ API response recebida")

            val forecast = parseApiResponse(JSONObject(response), cityName)

            // Salva no cache (resposta bruta + cidade do Geocoder)
            val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("api_response", response)
                putString("city_name", cityName)
                putLong("cache_time", System.currentTimeMillis())
                apply()
            }

            forecast
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na API: ${e.message}", e)
            getDefaultForecast()
        }
    }

    private fun parseApiResponse(json: JSONObject, cityName: String?): WeatherForecast {
        val currentJson = json.getJSONObject("current")

        val temperature = currentJson.getDouble("temperature_2m").toFloat()
        val weatherCode = currentJson.getInt("weather_code")
        val humidity = currentJson.getInt("relative_humidity_2m")
        val windSpeed = currentJson.getDouble("wind_speed_10m").toFloat()

        // Usa horário local para dia/noite
        val localHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isDaytime = localHour in 6..17

        val dailyJson = json.getJSONObject("daily")
        val tempMax = dailyJson.getJSONArray("temperature_2m_max")
            .getDouble(0).toFloat()
        val tempMin = dailyJson.getJSONArray("temperature_2m_min")
            .getDouble(0).toFloat()

        val current = WeatherData(
            weatherCode = weatherCode,
            temperature = temperature,
            temperatureMin = tempMin,
            temperatureMax = tempMax,
            isDaytime = isDaytime,
            humidity = humidity,
            windSpeed = windSpeed,
            cityName = cityName
        )

        // Parsear previsão diária
        val times = dailyJson.getJSONArray("time")
        val weatherCodes = dailyJson.getJSONArray("weather_code")
        val tempMaxArray = dailyJson.getJSONArray("temperature_2m_max")
        val tempMinArray = dailyJson.getJSONArray("temperature_2m_min")

        val brLocale = Locale("pt", "BR")
        val weekdayFormat = SimpleDateFormat("EEE", brLocale)
        val dateParser = SimpleDateFormat("yyyy-MM-dd", brLocale)

        val dailyList = mutableListOf<DailyForecast>()
        for (i in 0 until times.length()) {
            val dateStr = times.getString(i)
            val parsedDate = dateParser.parse(dateStr)
            val weekday = if (parsedDate != null) {
                weekdayFormat.format(parsedDate).replaceFirstChar {
                    it.titlecase(brLocale)
                }
            } else {
                dateStr
            }

            dailyList.add(
                DailyForecast(
                    date = dateStr,
                    weekday = weekday,
                    weatherCode = weatherCodes.getInt(i),
                    tempMin = tempMinArray.getDouble(i).toFloat(),
                    tempMax = tempMaxArray.getDouble(i).toFloat()
                )
            )
        }

        return WeatherForecast(
            current = current,
            daily = dailyList
        )
    }

    private fun getLastKnownLocation(context: Context): Location? {
        return try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val providers = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER
            )

            for (provider in providers) {
                try {
                    val location =
                        locationManager.getLastKnownLocation(provider)

                    if (location != null) {
                        return location
                    }
                } catch (_: SecurityException) {
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro localização: ${e.message}")
            null
        }
    }

    private fun getCityName(
        context: Context,
        lat: Double,
        lon: Double
    ): String? {
        return try {
            val geocoder = Geocoder(context, Locale("pt", "BR"))

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]

                Log.d(TAG, "🏙️ Cidade: ${addr.locality}")

                addr.locality
                    ?: addr.subAdminArea
                    ?: addr.adminArea
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Geocoder: ${e.message}")
            null
        }
    }

    private fun getDefaultForecast(): WeatherForecast {
        val localHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val current = WeatherData(
            weatherCode = 0,
            temperature = 20f,
            temperatureMin = 15f,
            temperatureMax = 25f,
            isDaytime = localHour in 6..17,
            humidity = 60,
            windSpeed = 10f,
            cityName = null
        )

        return WeatherForecast(
            current = current,
            daily = emptyList()
        )
    }
}
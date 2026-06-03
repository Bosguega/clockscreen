package br.com.clockscreen

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object WeatherService {

    private const val TAG = "WeatherService"
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 min
    private const val HTTP_TIMEOUT_MS = 10_000 // 10s

    suspend fun getWeather(context: Context): WeatherData? {
        return getForecast(context)?.current
    }

    suspend fun getForecast(context: Context): WeatherForecast? {
        val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
        val cacheTime = prefs.getLong("cache_time", 0)
        val currentTime = System.currentTimeMillis()
        val cachedCity = prefs.getString("city_name", null)

        val forceRefresh = cachedCity?.contains("fallback", ignoreCase = true) == true
                || cachedCity?.contains("padrão", ignoreCase = true) == true
                || cachedCity == null

        if (currentTime - cacheTime < CACHE_DURATION_MS && !forceRefresh) {
            Log.d(TAG, "📦 Usando dados do cache")
            val cachedResponse = prefs.getString("api_response", null)
            if (cachedResponse != null) {
                return try {
                    parseApiResponse(JSONObject(cachedResponse), cachedCity)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao ler cache: ${e.message}", e)
                    null
                }
            }
        }

        if (forceRefresh) {
            Log.d(TAG, "🔄 Forçando atualização (cache inválido ou fallback)")
        }

        return try {
            val location = getCurrentLocation(context)

            if (location == null) {
                Log.w(TAG, "⚠️ Sem localização, usando fallback")
                fetchWeather(
                    lat = -23.55,
                    lon = -46.63,
                    cityName = context.getString(R.string.fallback_city),
                    context = context
                )
            } else {
                Log.d(TAG, "📍 lat=${location.latitude}, lon=${location.longitude}")

                val cityName = getCityName(context, location.latitude, location.longitude)

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
        // Usa campo is_day retornado diretamente pela API (1 = dia, 0 = noite)
        val isDaytime = currentJson.optInt("is_day", 1) == 1

        val dailyJson = json.getJSONObject("daily")
        val tempMax = dailyJson.getJSONArray("temperature_2m_max").getDouble(0).toFloat()
        val tempMin = dailyJson.getJSONArray("temperature_2m_min").getDouble(0).toFloat()

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
                weekdayFormat.format(parsedDate).replaceFirstChar { it.titlecase(brLocale) }
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

        return WeatherForecast(current = current, daily = dailyList).also {
            Log.d(TAG, "✅ Forecast criado - cidade: ${current.cityName ?: "null"}")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): android.location.Location? {
        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            val fresh = suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { cts.cancel() }

                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        cont.resume(location)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ FusedLocation falhou: ${e.message}")
                        cont.resume(null)
                    }
            }

            if (fresh != null) return fresh

            // Fallback: última localização conhecida em cache
            Log.w(TAG, "⚠️ getCurrentLocation retornou null, tentando lastLocation")
            suspendCancellableCoroutine { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location == null) Log.w(TAG, "⚠️ lastLocation também null")
                        cont.resume(location)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ lastLocation falhou: ${e.message}")
                        cont.resume(null)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro localização: ${e.message}")
            null
        }
    }

    private suspend fun getCityName(context: Context, lat: Double, lon: Double): String? {
        return try {
            Log.d(TAG, "🌐 Buscando cidade para lat=$lat, lon=$lon")
            val geocoder = Geocoder(context, Locale("pt", "BR"))

            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(results: MutableList<Address>) {
                            cont.resume(results)
                        }
                        override fun onError(errorMessage: String?) {
                            Log.e(TAG, "❌ Geocoder.onError: $errorMessage")
                            cont.resume(emptyList())
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)
            }

            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                Log.d(TAG, "✅ Cidade definida: $cityName")
                cityName
            } else {
                Log.w(TAG, "⚠️ Geocoder não retornou endereços")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Geocoder: ${e.message}")
            null
        }
    }

    private fun getDefaultForecast(): WeatherForecast {
        val current = WeatherData(
            weatherCode = 0,
            temperature = 20f,
            temperatureMin = 15f,
            temperatureMax = 25f,
            isDaytime = true,
            humidity = 60,
            windSpeed = 10f,
            cityName = null
        )
        return WeatherForecast(current = current, daily = emptyList())
    }
}

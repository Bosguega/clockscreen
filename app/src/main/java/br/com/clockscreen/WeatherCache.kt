package br.com.clockscreen

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension property for DataStore on Context
private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_cache")

class WeatherCache(private val context: Context) {

    companion object {
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 min

        private val KEY_API_RESPONSE = stringPreferencesKey("api_response")
        private val KEY_CITY_NAME = stringPreferencesKey("city_name")
        private val KEY_CACHE_TIME = longPreferencesKey("cache_time")
        private val KEY_MANUAL_CITY = stringPreferencesKey("manual_city_name")
        private val KEY_MANUAL_CITY_LAT = stringPreferencesKey("manual_city_lat")
        private val KEY_MANUAL_CITY_LON = stringPreferencesKey("manual_city_lon")
        private val KEY_USE_MANUAL_CITY = stringPreferencesKey("use_manual_city")
    }

    data class CachedWeather(
        val apiResponse: String?,
        val cityName: String?,
        val cacheTime: Long
    )

    data class ManualCityConfig(
        val cityName: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val isEnabled: Boolean = false
    )

    private val prefs: Flow<Preferences> = context.weatherDataStore.data

    suspend fun getCachedWeather(): CachedWeather {
        val preferences = prefs.first()
        return CachedWeather(
            apiResponse = preferences[KEY_API_RESPONSE],
            cityName = preferences[KEY_CITY_NAME],
            cacheTime = preferences[KEY_CACHE_TIME] ?: 0L
        )
    }

    fun observeCachedWeather(): Flow<CachedWeather> {
        return prefs.map { preferences ->
            CachedWeather(
                apiResponse = preferences[KEY_API_RESPONSE],
                cityName = preferences[KEY_CITY_NAME],
                cacheTime = preferences[KEY_CACHE_TIME] ?: 0L
            )
        }
    }

    suspend fun saveWeather(apiResponse: String, cityName: String?) {
        context.weatherDataStore.edit { preferences ->
            preferences[KEY_API_RESPONSE] = apiResponse
            preferences[KEY_CITY_NAME] = cityName
            preferences[KEY_CACHE_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun isCacheValid(): Boolean {
        val cache = getCachedWeather()
        val isExpired = System.currentTimeMillis() - cache.cacheTime >= CACHE_DURATION_MS
        val isFallback = cache.cityName?.contains("fallback", ignoreCase = true) == true
                || cache.cityName?.contains("padrão", ignoreCase = true) == true
                || cache.cityName == null
        return !isExpired && !isFallback
    }

    suspend fun isForceRefresh(): Boolean {
        val cache = getCachedWeather()
        return cache.cityName?.contains("fallback", ignoreCase = true) == true
                || cache.cityName?.contains("padrão", ignoreCase = true) == true
                || cache.cityName == null
    }

    // Métodos para cidade manual

    suspend fun getManualCityConfig(): ManualCityConfig {
        val preferences = prefs.first()
        return ManualCityConfig(
            cityName = preferences[KEY_MANUAL_CITY],
            latitude = preferences[KEY_MANUAL_CITY_LAT]?.toDoubleOrNull(),
            longitude = preferences[KEY_MANUAL_CITY_LON]?.toDoubleOrNull(),
            isEnabled = preferences[KEY_USE_MANUAL_CITY] ?: false
        )
    }

    fun observeManualCityConfig(): Flow<ManualCityConfig> {
        return prefs.map { preferences ->
            ManualCityConfig(
                cityName = preferences[KEY_MANUAL_CITY],
                latitude = preferences[KEY_MANUAL_CITY_LAT]?.toDoubleOrNull(),
                longitude = preferences[KEY_MANUAL_CITY_LON]?.toDoubleOrNull(),
                isEnabled = preferences[KEY_USE_MANUAL_CITY] ?: false
            )
        }
    }

    suspend fun saveManualCityConfig(config: ManualCityConfig) {
        context.weatherDataStore.edit { preferences ->
            preferences[KEY_MANUAL_CITY] = config.cityName
            preferences[KEY_MANUAL_CITY_LAT] = config.latitude?.toString()
            preferences[KEY_MANUAL_CITY_LON] = config.longitude?.toString()
            preferences[KEY_USE_MANUAL_CITY] = config.isEnabled
        }
    }

    suspend fun clearCache() {
        context.weatherDataStore.edit { preferences ->
            preferences.remove(KEY_API_RESPONSE)
            preferences.remove(KEY_CITY_NAME)
            preferences.remove(KEY_CACHE_TIME)
        }
    }
}
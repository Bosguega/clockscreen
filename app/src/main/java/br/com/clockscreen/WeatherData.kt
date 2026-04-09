package br.com.clockscreen

data class WeatherData(
    val weatherCode: Int,
    val temperature: Float,
    val temperatureMin: Float,
    val temperatureMax: Float,
    val isDaytime: Boolean,
    val humidity: Int,
    val windSpeed: Float,
    val cityName: String?
)

data class DailyForecast(
    val date: String,
    val weekday: String,
    val weatherCode: Int,
    val tempMin: Float,
    val tempMax: Float
)

data class WeatherForecast(
    val current: WeatherData,
    val daily: List<DailyForecast>
)

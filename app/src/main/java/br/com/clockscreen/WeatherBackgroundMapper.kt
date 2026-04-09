package br.com.clockscreen

object WeatherBackgroundMapper {

    /**
     * Mapeia WMO Weather Code para drawable de background
     * https://open-meteo.com/en/docs
     */
    fun getBackgroundResource(weatherCode: Int, isDaytime: Boolean): Int {
        return when (weatherCode) {
            0 -> {
                // Céu limpo
                if (isDaytime) R.drawable.bg_weather_sunny
                else R.drawable.bg_weather_clear_night
            }
            in 1..3 -> {
                // Parcialmente nublado / nublado
                if (isDaytime) R.drawable.bg_weather_cloudy
                else R.drawable.bg_weather_cloudy_night
            }
            45, 48 -> {
                // Neblina
                R.drawable.bg_weather_fog
            }
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> {
                // Chuva (várias intensidades)
                if (isDaytime) R.drawable.bg_weather_rainy
                else R.drawable.bg_weather_rainy_night
            }
            71, 73, 75, 77, 85, 86 -> {
                // Neve
                R.drawable.bg_weather_snow
            }
            95, 96, 99 -> {
                // Tempestade
                R.drawable.bg_weather_thunder
            }
            else -> {
                // Default
                if (isDaytime) R.drawable.bg_weather_cloudy
                else R.drawable.bg_weather_cloudy_night
            }
        }
    }

    /**
     * Retorna ícone/emoji baseado no clima e horário.
     * isDaytime=true é o default para chamadas de previsão diária.
     */
    fun getWeatherEmoji(weatherCode: Int, isDaytime: Boolean = true): String {
        return when (weatherCode) {
            // Céu limpo
            0 -> if (isDaytime) "☀️" else "🌙"
            // Predominantemente limpo
            1 -> if (isDaytime) "🌤️" else "🌙"
            // Parcialmente nublado
            2 -> if (isDaytime) "⛅" else "☁️"
            // Nublado
            3 -> "☁️"
            // Neblina
            45, 48 -> "🌫️"
            // Garoa leve/moderada (sol ainda visível de dia)
            51, 53 -> if (isDaytime) "🌦️" else "🌧️"
            // Garoa intensa / congelante
            in 55..57 -> "🌧️"
            // Chuva
            in 61..67 -> "🌧️"
            // Neve leve/moderada
            in 71..73 -> "🌨️"
            // Neve forte / granizo fino
            75, 77 -> "❄️"
            // Pancadas leves (sol entre nuvens de dia)
            80 -> if (isDaytime) "🌦️" else "🌧️"
            // Pancadas moderadas/fortes
            81, 82 -> "🌧️"
            // Neve fraca/forte
            85, 86 -> "🌨️"
            // Tempestade
            95 -> "⛈️"
            // Tempestade com granizo
            96, 99 -> "⛈️"
            else -> "🌡️"
        }
    }

    /**
     * Retorna descrição textual do clima (WMO Weather Code)
     */
    fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Céu limpo"
            1 -> "Predominantemente limpo"
            2 -> "Parcialmente nublado"
            3 -> "Nublado"
            45 -> "Neblina"
            48 -> "Neblina com geada"
            51 -> "Garoa leve"
            53 -> "Garoa moderada"
            55 -> "Garoa intensa"
            56, 57 -> "Garoa congelante"
            61 -> "Chuva leve"
            63 -> "Chuva moderada"
            65 -> "Chuva forte"
            66, 67 -> "Chuva congelante"
            71 -> "Neve leve"
            73 -> "Neve moderada"
            75 -> "Neve forte"
            77 -> "Granizo fino"
            80 -> "Pancadas leves"
            81 -> "Pancadas moderadas"
            82 -> "Pancadas fortes"
            85 -> "Neve fraca"
            86 -> "Neve forte"
            95 -> "Tempestade"
            96, 99 -> "Tempestade com granizo"
            else -> "Indisponível"
        }
    }

    /**
     * Retorna cores de texto baseadas no background
     */
    fun getTextColorPrimary(isDaytime: Boolean, weatherCode: Int): String {
        return if (isDaytime) "#FFFFFF" else "#E8E8E8"
    }

    fun getTextColorSecondary(isDaytime: Boolean, weatherCode: Int): String {
        return if (isDaytime) "#F0F0F0" else "#B8B8B8"
    }
}

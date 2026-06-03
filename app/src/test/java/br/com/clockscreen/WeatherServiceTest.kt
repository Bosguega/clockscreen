package br.com.clockscreen

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitários para WeatherService.
 *
 * Testa o parser de resposta da API Open-Meteo (parseApiResponse)
 * sem chamadas de rede reais.
 *
 * Como parseApiResponse é privado, testamos via reflection para garantir
 * que o parser funciona corretamente com vários cenários de resposta JSON.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherServiceTest {

    /**
     * Usa reflection para chamar o método privado parseApiResponse
     */
    private fun invokeParseApiResponse(json: JSONObject, cityName: String?): WeatherForecast {
        val method = WeatherService::class.java.getDeclaredMethod(
            "parseApiResponse",
            JSONObject::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(WeatherService, json, cityName) as WeatherForecast
    }

    @Test
    fun `parseApiResponse_com_dados_validos_retorna_forecast_correto`() {
        val jsonStr = """
        {
            "current": {
                "temperature_2m": 25.5,
                "relative_humidity_2m": 72,
                "weather_code": 2,
                "wind_speed_10m": 12.3,
                "is_day": 1
            },
            "daily": {
                "time": ["2026-06-03", "2026-06-04", "2026-06-05"],
                "weather_code": [2, 1, 3],
                "temperature_2m_max": [28.0, 26.5, 24.0],
                "temperature_2m_min": [18.0, 17.0, 16.5]
            }
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val forecast = invokeParseApiResponse(json, "São Paulo")

        assertNotNull("Forecast não deve ser null", forecast)

        // Verifica dados do current
        with(forecast.current) {
            assertEquals("Temperatura deve ser 25.5", 25.5f, temperature, 0.01f)
            assertEquals("Umidade deve ser 72", 72, humidity)
            assertEquals("Weather code deve ser 2", 2, weatherCode)
            assertEquals("Velocidade vento deve ser 12.3", 12.3f, windSpeed, 0.01f)
            assertEquals("isDaytime deve ser true", true, isDaytime)
            assertEquals("Cidade deve ser São Paulo", "São Paulo", cityName)
            assertEquals("Temp min deve ser 18.0", 18.0f, temperatureMin, 0.01f)
            assertEquals("Temp max deve ser 28.0", 28.0f, temperatureMax, 0.01f)
        }

        // Verifica previsão diária
        assertEquals("Deve ter 3 dias de previsão", 3, forecast.daily.size)

        with(forecast.daily[0]) {
            assertEquals("Data deve ser 2026-06-03", "2026-06-03", date)
            assertEquals("Weather code deve ser 2", 2, weatherCode)
            assertEquals("Temp min deve ser 18.0", 18.0f, tempMin, 0.01f)
            assertEquals("Temp max deve ser 28.0", 28.0f, tempMax, 0.01f)
        }

        with(forecast.daily[1]) {
            assertEquals("Data deve ser 2026-06-04", "2026-06-04", date)
            assertEquals("Weather code deve ser 1", 1, weatherCode)
        }

        with(forecast.daily[2]) {
            assertEquals("Data deve ser 2026-06-05", "2026-06-05", date)
            assertEquals("Weather code deve ser 3", 3, weatherCode)
        }
    }

    @Test
    fun `parseApiResponse_cityName_null_aceita`() {
        val jsonStr = """
        {
            "current": {
                "temperature_2m": 22.0,
                "relative_humidity_2m": 60,
                "weather_code": 0,
                "wind_speed_10m": 5.0,
                "is_day": 1
            },
            "daily": {
                "time": ["2026-06-03"],
                "weather_code": [0],
                "temperature_2m_max": [27.0],
                "temperature_2m_min": [16.0]
            }
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val forecast = invokeParseApiResponse(json, null)

        assertNotNull("Forecast não deve ser null", forecast)
        assertNull("cityName deve ser null", forecast.current.cityName)
    }

    @Test
    fun `parseApiResponse_is_day_0_define_noite`() {
        val jsonStr = """
        {
            "current": {
                "temperature_2m": 18.0,
                "relative_humidity_2m": 80,
                "weather_code": 45,
                "wind_speed_10m": 8.0,
                "is_day": 0
            },
            "daily": {
                "time": ["2026-06-03"],
                "weather_code": [45],
                "temperature_2m_max": [22.0],
                "temperature_2m_min": [14.0]
            }
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val forecast = invokeParseApiResponse(json, "São Paulo")

        assertEquals("isDaytime deve ser false quando is_day=0", false, forecast.current.isDaytime)
    }

    @Test
    fun `parseApiResponse_is_day_ausente_padrao_dia`() {
        val jsonStr = """
        {
            "current": {
                "temperature_2m": 30.0,
                "relative_humidity_2m": 50,
                "weather_code": 0,
                "wind_speed_10m": 3.0
            },
            "daily": {
                "time": ["2026-06-03"],
                "weather_code": [0],
                "temperature_2m_max": [35.0],
                "temperature_2m_min": [22.0]
            }
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val forecast = invokeParseApiResponse(json, "Rio de Janeiro")

        assertEquals("isDaytime deve ser true quando is_day está ausente (padrão)",
            true, forecast.current.isDaytime)
    }

    @Test
    fun `parseApiResponse_com_daily_vazio_funciona`() {
        val jsonStr = """
        {
            "current": {
                "temperature_2m": 20.0,
                "relative_humidity_2m": 65,
                "weather_code": 3,
                "wind_speed_10m": 10.0,
                "is_day": 0
            },
            "daily": {
                "time": [],
                "weather_code": [],
                "temperature_2m_max": [],
                "temperature_2m_min": []
            }
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val forecast = invokeParseApiResponse(json, "Belo Horizonte")

        assertNotNull("Forecast não deve ser null", forecast)
        assertEquals("Daily deve estar vazio", 0, forecast.daily.size)
        assertNotNull("Current não deve ser null", forecast.current)
        assertEquals("Cidade deve ser Belo Horizonte", "Belo Horizonte", forecast.current.cityName)
    }

    @Test
    fun `parseApiResponse_temperatura_negativa_funciona`() {
        val jsonStr = """
        {
            "current": {
                "temperature_2m": -5.0,
                "relative_humidity_2m": 85,
                "weather_code": 71,
                "wind_speed_10m": 20.0,
                "is_day": 0
            },
            "daily": {
                "time": ["2026-06-03"],
                "weather_code": [71],
                "temperature_2m_max": [-2.0],
                "temperature_2m_min": [-8.0]
            }
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val forecast = invokeParseApiResponse(json, "Gramado")

        assertEquals("Temperatura deve ser -5.0", -5.0f, forecast.current.temperature, 0.01f)
        assertEquals("Temp min deve ser -8.0", -8.0f, forecast.daily[0].tempMin, 0.01f)
        assertEquals("Temp max deve ser -2.0", -2.0f, forecast.daily[0].tempMax, 0.01f)
    }

    @Test
    fun `parseApiResponse_dia_da_semana_eh_formatado_corretamente`() {
        // 2026-06-03 é uma quarta-feira
        val jsonStr = """
        {
            "current": {
                "temperature_2m": 25.0,
                "relative_humidity_2m": 60,
                "weather_code": 0,
                "wind_speed_10m": 5.0,
                "is_day": 1
            },
            "daily": {
                "time": ["2026-06-03"],
                "weather_code": [0],
                "temperature_2m_max": [30.0],
                "temperature_2m_min": [18.0]
            }
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val forecast = invokeParseApiResponse(json, "São Paulo")

        val weekday = forecast.daily[0].weekday
        // O dia da semana em português pode variar entre "Qua", "qua", "Quarta", "quarta"
        // Só verifica que não está vazio e não é a data original
        assertNotNull("Weekday não deve ser null", weekday)
        assert(weekday.isNotBlank()) { "Weekday não deve estar em branco" }
        assertNotEquals("Weekday não deve ser a data original", "2026-06-03", weekday)
    }
}
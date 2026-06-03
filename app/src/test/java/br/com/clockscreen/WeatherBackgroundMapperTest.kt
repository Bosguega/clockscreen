package br.com.clockscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitários para WeatherBackgroundMapper.
 *
 * Verifica o mapeamento de WMO Weather Codes para:
 * - Background resources (drawables) - verifica IDs como inteiros
 * - Emojis
 * - Descrições textuais
 * - Cores de texto
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherBackgroundMapperTest {

    // --- Testes: Background Resources ---

    @Test
    fun `weather_code_0_dia_retorna_background_valido`() {
        val resource = WeatherBackgroundMapper.getBackgroundResource(weatherCode = 0, isDaytime = true)
        assertNotEquals("Background resource deve ser um ID válido", 0, resource)
    }

    @Test
    fun `weather_code_0_dia_e_noite_retornam_backgrounds_diferentes`() {
        val day = WeatherBackgroundMapper.getBackgroundResource(0, isDaytime = true)
        val night = WeatherBackgroundMapper.getBackgroundResource(0, isDaytime = false)
        assertNotEquals("Dia e noite para código 0 devem ter backgrounds diferentes", day, night)
    }

    @Test
    fun `weather_code_0_noite_retorna_clear_night_background`() {
        val resource = WeatherBackgroundMapper.getBackgroundResource(weatherCode = 0, isDaytime = false)
        assertEquals(R.drawable.bg_weather_clear_night, resource)
    }

    @Test
    fun `weather_code_1_a_3_dia_retorna_cloudy_background`() {
        for (code in 1..3) {
            val resource = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = true)
            assertEquals("Código $code (parcialmente nublado) de dia deve retornar bg_weather_cloudy",
                R.drawable.bg_weather_cloudy, resource)
        }
    }

    @Test
    fun `weather_code_1_a_3_noite_retorna_cloudy_night_background`() {
        for (code in 1..3) {
            val resource = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = false)
            assertEquals("Código $code (parcialmente nublado) de noite deve retornar bg_weather_cloudy_night",
                R.drawable.bg_weather_cloudy_night, resource)
        }
    }

    @Test
    fun `weather_code_45_e_48_retorna_fog_background`() {
        for (code in listOf(45, 48)) {
            val resource = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = true)
            assertEquals("Código $code (neblina) deve retornar bg_weather_fog",
                R.drawable.bg_weather_fog, resource)
        }
    }

    @Test
    fun `weather_code_chuva_dia_retorna_rainy_background`() {
        val rainyCodes = listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
        for (code in rainyCodes) {
            val resource = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = true)
            assertEquals("Código $code (chuva) de dia deve retornar bg_weather_rainy",
                R.drawable.bg_weather_rainy, resource)
        }
    }

    @Test
    fun `weather_code_chuva_noite_retorna_rainy_night_background`() {
        val rainyCodes = listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
        for (code in rainyCodes) {
            val resource = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = false)
            assertEquals("Código $code (chuva) de noite deve retornar bg_weather_rainy_night",
                R.drawable.bg_weather_rainy_night, resource)
        }
    }

    @Test
    fun `weather_code_neve_retorna_snow_background`() {
        val snowCodes = listOf(71, 73, 75, 77, 85, 86)
        for (code in snowCodes) {
            val resource = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = true)
            assertEquals("Código $code (neve) deve retornar bg_weather_snow",
                R.drawable.bg_weather_snow, resource)
        }
    }

    @Test
    fun `weather_code_tempestade_retorna_thunder_background`() {
        val thunderCodes = listOf(95, 96, 99)
        for (code in thunderCodes) {
            val resource = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = true)
            assertEquals("Código $code (tempestade) deve retornar bg_weather_thunder",
                R.drawable.bg_weather_thunder, resource)
        }
    }

    @Test
    fun `weather_code_desconhecido_retorna_cloudy_default`() {
        val unknownCodes = listOf(-1, 10, 100, 200)
        for (code in unknownCodes) {
            val resourceDay = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = true)
            assertEquals("Código $code desconhecido de dia deve retornar bg_weather_cloudy",
                R.drawable.bg_weather_cloudy, resourceDay)

            val resourceNight = WeatherBackgroundMapper.getBackgroundResource(code, isDaytime = false)
            assertEquals("Código $code desconhecido de noite deve retornar bg_weather_cloudy_night",
                R.drawable.bg_weather_cloudy_night, resourceNight)
        }
    }

    @Test
    fun `backgrounds_de_dia_sao_diferentes_de_noite`() {
        // Verifica que backgrounds de dia/noite para o mesmo tipo são diferentes
        val daySunny = WeatherBackgroundMapper.getBackgroundResource(0, true)
        val nightSunny = WeatherBackgroundMapper.getBackgroundResource(0, false)
        assertNotEquals("Sunny day vs night", daySunny, nightSunny)

        val dayCloudy = WeatherBackgroundMapper.getBackgroundResource(2, true)
        val nightCloudy = WeatherBackgroundMapper.getBackgroundResource(2, false)
        assertNotEquals("Cloudy day vs night", dayCloudy, nightCloudy)
    }

    // --- Testes: Emojis ---

    @Test
    fun `weather_code_0_dia_retorna_sol_emoji`() {
        val emoji = WeatherBackgroundMapper.getWeatherEmoji(weatherCode = 0, isDaytime = true)
        assertEquals("☀️", emoji)
    }

    @Test
    fun `weather_code_0_noite_retorna_lua_emoji`() {
        val emoji = WeatherBackgroundMapper.getWeatherEmoji(weatherCode = 0, isDaytime = false)
        assertEquals("🌙", emoji)
    }

    @Test
    fun `weather_code_3_retorna_nuvem_emoji_independente_horario`() {
        val emojiDay = WeatherBackgroundMapper.getWeatherEmoji(weatherCode = 3, isDaytime = true)
        val emojiNight = WeatherBackgroundMapper.getWeatherEmoji(weatherCode = 3, isDaytime = false)
        assertEquals("☁️", emojiDay)
        assertEquals("☁️", emojiNight)
    }

    @Test
    fun `weather_code_95_retorna_tempestade_emoji`() {
        val emoji = WeatherBackgroundMapper.getWeatherEmoji(weatherCode = 95)
        assertEquals("⛈️", emoji)
    }

    @Test
    fun `weather_code_61_retorna_chuva_emoji`() {
        val emoji = WeatherBackgroundMapper.getWeatherEmoji(61)
        assertEquals("🌧️", emoji)
    }

    @Test
    fun `weather_code_desconhecido_retorna_termometro_emoji`() {
        val emoji = WeatherBackgroundMapper.getWeatherEmoji(-1)
        assertEquals("🌡️", emoji)
    }

    // --- Testes: Descrições ---

    @Test
    fun `weather_code_0_retorna_ceu_limpo`() {
        val desc = WeatherBackgroundMapper.getWeatherDescription(0)
        assertEquals("Céu limpo", desc)
    }

    @Test
    fun `weather_code_3_retorna_nublado`() {
        val desc = WeatherBackgroundMapper.getWeatherDescription(3)
        assertEquals("Nublado", desc)
    }

    @Test
    fun `weather_code_45_retorna_neblina`() {
        val desc = WeatherBackgroundMapper.getWeatherDescription(45)
        assertEquals("Neblina", desc)
    }

    @Test
    fun `weather_code_51_retorna_garoa_leve`() {
        val desc = WeatherBackgroundMapper.getWeatherDescription(51)
        assertEquals("Garoa leve", desc)
    }

    @Test
    fun `weather_code_61_retorna_chuva_leve`() {
        val desc = WeatherBackgroundMapper.getWeatherDescription(61)
        assertEquals("Chuva leve", desc)
    }

    @Test
    fun `weather_code_95_retorna_tempestade`() {
        val desc = WeatherBackgroundMapper.getWeatherDescription(95)
        assertEquals("Tempestade", desc)
    }

    @Test
    fun `weather_code_96_retorna_tempestade_com_granizo`() {
        val desc = WeatherBackgroundMapper.getWeatherDescription(96)
        assertEquals("Tempestade com granizo", desc)
    }

    @Test
    fun `weather_code_desconhecido_retorna_indisponivel`() {
        val desc = WeatherBackgroundMapper.getWeatherDescription(-1)
        assertEquals("Indisponível", desc)
    }

    // --- Testes: Cores de Texto ---

    @Test
    fun `text_color_primary_dia_retorna_branco`() {
        val color = WeatherBackgroundMapper.getTextColorPrimary(isDaytime = true, weatherCode = 0)
        assertEquals("#FFFFFF", color)
    }

    @Test
    fun `text_color_primary_noite_retorna_cinza_claro`() {
        val color = WeatherBackgroundMapper.getTextColorPrimary(isDaytime = false, weatherCode = 0)
        assertEquals("#E8E8E8", color)
    }

    @Test
    fun `text_color_secondary_dia_retorna_branco_amarelado`() {
        val color = WeatherBackgroundMapper.getTextColorSecondary(isDaytime = true, weatherCode = 0)
        assertEquals("#F0F0F0", color)
    }

    @Test
    fun `text_color_secondary_noite_retorna_cinza_medio`() {
        val color = WeatherBackgroundMapper.getTextColorSecondary(isDaytime = false, weatherCode = 0)
        assertEquals("#B8B8B8", color)
    }

    @Test
    fun `cores_primarias_sao_diferentes_das_secundarias`() {
        val primaryDay = WeatherBackgroundMapper.getTextColorPrimary(true, 0)
        val secondaryDay = WeatherBackgroundMapper.getTextColorSecondary(true, 0)
        assertNotEquals("Primary deve ser diferente de secondary", primaryDay, secondaryDay)
    }
}
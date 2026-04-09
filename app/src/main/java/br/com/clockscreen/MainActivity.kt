package br.com.clockscreen

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.clockscreen.databinding.ActivityMainBinding
import br.com.clockscreen.databinding.ItemForecastBinding
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionUtils.hasAlarmPermission(this)) {
            DateAlarmScheduler.scheduleMidnightUpdate(this)
            Toast.makeText(this, getString(R.string.alarm_permission_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.alarm_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any { it.value }
        if (granted) {
            loadWeatherData()
        } else {
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicita permissão de localização se necessário
        requestLocationPermission()

        // Carrega dados do clima
        loadWeatherData()

        // Agenda atualização da data à meia-noite (fuso local)
        scheduleMidnightUpdateWithPermissionCheck()

        binding.btnScreenSaver.setOnClickListener {
            startActivity(Intent(this, ScreenSaverActivity::class.java))
        }
    }

    private fun requestLocationPermission() {
        // minSdk 24 >= API 23 (M), checkSelfPermission é sempre disponível
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun loadWeatherData() {
        thread(start = true) {
            val forecast = WeatherService.getForecast(this)
            runOnUiThread {
                if (forecast != null) {
                    displayWeatherForecast(forecast)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.weather_load_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun displayWeatherForecast(forecast: WeatherForecast) {
        val current = forecast.current

        // Cidade
        binding.tvCity.text = current.cityName ?: getString(R.string.location_unavailable)

        // Ícone e descrição
        binding.tvWeatherIcon.text = WeatherBackgroundMapper.getWeatherEmoji(current.weatherCode, current.isDaytime)
        binding.tvWeatherDesc.text = WeatherBackgroundMapper.getWeatherDescription(current.weatherCode)

        // Temperatura
        binding.tvTemperature.text = "${current.temperature.toInt()}°C"

        // Umidade
        binding.tvHumidity.text = "${current.humidity}%"

        // Vento
        binding.tvWind.text = "${current.windSpeed.toInt()} km/h"

        // Min/Max
        binding.tvTempRange.text = "${current.temperatureMin.toInt()}° / ${current.temperatureMax.toInt()}°"

        // Previsão 7 dias
        binding.forecastContainer.removeAllViews()
        for (daily in forecast.daily) {
            val itemBinding = ItemForecastBinding.inflate(
                LayoutInflater.from(this),
                binding.forecastContainer,
                false
            )

            itemBinding.tvForecastDay.text = daily.weekday
            itemBinding.tvForecastIcon.text = WeatherBackgroundMapper.getWeatherEmoji(daily.weatherCode)
            itemBinding.tvForecastDesc.text = WeatherBackgroundMapper.getWeatherDescription(daily.weatherCode)
            itemBinding.tvForecastTemp.text = "${daily.tempMin.toInt()}° / ${daily.tempMax.toInt()}°"

            binding.forecastContainer.addView(itemBinding.root)
        }
    }

    private fun scheduleMidnightUpdateWithPermissionCheck() {
        if (PermissionUtils.hasAlarmPermission(this)) {
            DateAlarmScheduler.scheduleMidnightUpdate(this)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                requestAlarmPermissionLauncher.launch(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarrega dados do clima ao voltar para a activity
        loadWeatherData()
    }
}

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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.clockscreen.databinding.ActivityMainBinding
import br.com.clockscreen.databinding.ItemForecastBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels()

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
            viewModel.load()
        } else {
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestLocationPermission()
        scheduleMidnightUpdateWithPermissionCheck()

        lifecycleScope.launch {
            viewModel.forecast.collect { forecast ->
                if (forecast != null) displayWeatherForecast(forecast)
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.weather_load_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        lifecycleScope.launch {
            viewModel.manualCityConfig.collect { config ->
                if (config.isEnabled) {
                    binding.btnScreenSaver.text = getString(R.string.manual_city_active, config.cityName ?: "")
                } else {
                    binding.btnScreenSaver.text = getString(R.string.start_screen_saver)
                }
            }
        }

        binding.btnScreenSaver.setOnClickListener {
            startActivity(Intent(this, ScreenSaverActivity::class.java))
        }

        binding.tvCity.setOnClickListener {
            showCityConfigDialog()
        }
    }

    private fun requestLocationPermission() {
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

    private fun displayWeatherForecast(forecast: WeatherForecast) {
        val current = forecast.current

        binding.tvCity.text = current.cityName ?: getString(R.string.location_unavailable)
        binding.tvWeatherIcon.text = WeatherBackgroundMapper.getWeatherEmoji(current.weatherCode, current.isDaytime)
        binding.tvWeatherDesc.text = WeatherBackgroundMapper.getWeatherDescription(current.weatherCode)
        binding.tvTemperature.text = "${current.temperature.toInt()}°C"
        binding.tvHumidity.text = "${current.humidity}%"
        binding.tvWind.text = "${current.windSpeed.toInt()} km/h"
        binding.tvTempRange.text = "${current.temperatureMin.toInt()}° / ${current.temperatureMax.toInt()}°"

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

    private fun showCityConfigDialog() {
        val config = viewModel.manualCityConfig.value
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_city_config, null)
        val etCityName = dialogView.findViewById<TextInputEditText>(R.id.et_city_name)
        val etLatitude = dialogView.findViewById<TextInputEditText>(R.id.et_latitude)
        val etLongitude = dialogView.findViewById<TextInputEditText>(R.id.et_longitude)

        if (config.isEnabled) {
            etCityName.setText(config.cityName)
            etLatitude.setText(config.latitude?.toString())
            etLongitude.setText(config.longitude?.toString())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.city_config_title)
            .setView(dialogView)
            .setPositiveButton(R.string.city_config_save) { _, _ ->
                val cityName = etCityName.text?.toString()?.trim()
                val latText = etLatitude.text?.toString()?.trim()
                val lonText = etLongitude.text?.toString()?.trim()

                if (cityName.isNullOrBlank() || latText.isNullOrBlank() || lonText.isNullOrBlank()) {
                    Toast.makeText(this, R.string.city_config_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val lat = latText.toDoubleOrNull()
                val lon = lonText.toDoubleOrNull()
                if (lat == null || lon == null) {
                    Toast.makeText(this, R.string.city_config_invalid_coords, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.setManualCity(cityName, lat, lon, enabled = true)
                Toast.makeText(this, getString(R.string.city_config_saved, cityName), Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(R.string.city_config_disable) { _, _ ->
                viewModel.disableManualCity()
                Toast.makeText(this, R.string.city_config_disabled, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
        viewModel.load()
    }
}

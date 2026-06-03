package br.com.clockscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val _forecast = MutableStateFlow<WeatherForecast?>(null)
    val forecast: StateFlow<WeatherForecast?> = _forecast

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // SharedFlow com replay=0: eventos de erro não são re-emitidos ao recriar a Activity
    private val _error = MutableSharedFlow<Unit>(replay = 0)
    val error: SharedFlow<Unit> = _error

    fun load() {
        if (_isLoading.value) return  // evita requisições concorrentes duplicadas
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val result = WeatherService.getForecast(getApplication())
            _forecast.value = result
            if (result == null) _error.emit(Unit)
            _isLoading.value = false
        }
    }
}

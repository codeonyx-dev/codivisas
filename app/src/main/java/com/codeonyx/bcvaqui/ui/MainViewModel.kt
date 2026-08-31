package com.codeonyx.bcvaqui.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.local.UserPreferences
import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import com.codeonyx.bcvaqui.data.model.FxDashboard
import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.data.repository.FxRepository
import com.codeonyx.bcvaqui.widget.Widgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = PreferencesRepository(application)
    private val repository = FxRepository(preferencesRepository)

    val preferences: StateFlow<UserPreferences> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(15_000), UserPreferences())

    private val _dashboard = MutableStateFlow<FxDashboard?>(null)
    val dashboard: StateFlow<FxDashboard?> = _dashboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _history = MutableStateFlow<List<BcvRateResponse>>(emptyList())
    val history: StateFlow<List<BcvRateResponse>> = _history.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError.asStateFlow()

    init {
        refresh(forceNetwork = true)
    }

    fun refresh(forceNetwork: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshDashboard(forceNetwork).fold(
                onSuccess = { dashboard ->
                    _dashboard.value = dashboard
                    Widgets.updateAll(getApplication())
                },
                onFailure = {
                    repository.refreshDashboard(forceNetwork = false).onSuccess { cached ->
                        _dashboard.value = cached
                    }
                }
            )
            _isLoading.value = false
        }
    }

    fun loadHistory(days: Int = 30) {
        viewModelScope.launch {
            _historyLoading.value = true
            _historyError.value = null
            repository.fetchHistory(days).fold(
                onSuccess = { _history.value = it },
                onFailure = { _historyError.value = it.message ?: "No se pudo cargar el histórico" }
            )
            _historyLoading.value = false
        }
    }

    fun setSelectedCurrency(code: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedCurrency(code)
            Widgets.updateAll(getApplication())
        }
    }

    fun setNotificationInterval(hours: Int) {
        viewModelScope.launch {
            preferencesRepository.setNotificationIntervalHours(hours)
            com.codeonyx.bcvaqui.worker.WorkScheduler.schedulePeriodicRateCheck(getApplication())
            if (hours > 0) {
                com.codeonyx.bcvaqui.worker.WorkScheduler.enqueueImmediateRateCheck(getApplication())
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setHasSeenOnboarding(true)
        }
    }

    fun setAlertThresholdPercent(percent: Double) {
        viewModelScope.launch {
            preferencesRepository.setAlertThresholdPercent(percent)
            Widgets.updateAll(getApplication())
        }
    }

    fun addDondeCurrency(code: String) {
        viewModelScope.launch {
            val current = preferences.value.dondeCurrencies
            if (code !in current) {
                preferencesRepository.setDondeCurrencies(current + code)
            }
        }
    }

    fun removeDondeCurrency(code: String) {
        viewModelScope.launch {
            val current = preferences.value.dondeCurrencies
            if (current.size > 1) {
                preferencesRepository.setDondeCurrencies(current.filter { it != code })
            }
        }
    }

    fun toggleDondeCurrency(code: String) {
        viewModelScope.launch {
            val current = preferences.value.dondeCurrencies
            val next = if (code in current) {
                if (current.size <= 1) current else current.filter { it != code }
            } else {
                current + code
            }
            preferencesRepository.setDondeCurrencies(next)
        }
    }

    fun savePaymentContact(contact: PaymentContact) {
        viewModelScope.launch {
            preferencesRepository.upsertPaymentContact(contact)
        }
    }

    fun deletePaymentContact(id: String) {
        viewModelScope.launch {
            preferencesRepository.deletePaymentContact(id)
        }
    }
}

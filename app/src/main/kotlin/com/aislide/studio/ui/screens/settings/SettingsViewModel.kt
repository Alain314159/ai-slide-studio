package com.aislide.studio.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.data.local.SettingsDataStore
import com.aislide.studio.domain.usecase.ValidateApiKeyUseCase
import com.aislide.studio.domain.usecase.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val validateApiKeyUseCase: ValidateApiKeyUseCase
) : ViewModel() {

    val apiKey: StateFlow<String> = settings.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val providerId: StateFlow<String> = settings.providerId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.aislide.studio.data.remote.LlmProvider.GLM.id)

    val endpoint: StateFlow<String> = settings.endpoint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.aislide.studio.data.remote.LlmProvider.GLM.defaultEndpoint)

    val model: StateFlow<String> = settings.model
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.aislide.studio.data.remote.LlmProvider.GLM.defaultModel)

    private val _validating = MutableStateFlow(false)
    val validating: StateFlow<Boolean> = _validating.asStateFlow()

    private val _validation = MutableStateFlow<ValidationResult?>(null)
    val validation: StateFlow<ValidationResult?> = _validation.asStateFlow()

    fun setApiKey(value: String) {
        viewModelScope.launch { settings.setApiKey(value) }
    }
    fun setProviderId(value: String) {
        viewModelScope.launch { settings.setProviderId(value) }
    }
    fun setEndpoint(value: String) {
        viewModelScope.launch { settings.setEndpoint(value) }
    }
    fun setModel(value: String) {
        viewModelScope.launch { settings.setModel(value) }
    }

    fun validate() {
        if (_validating.value) return
        viewModelScope.launch {
            _validating.value = true
            _validation.value = null
            _validation.value = validateApiKeyUseCase()
            _validating.value = false
        }
    }

    fun clearValidation() { _validation.value = null }
}

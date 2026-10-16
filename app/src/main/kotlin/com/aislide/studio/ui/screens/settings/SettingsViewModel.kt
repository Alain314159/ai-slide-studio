package com.aislide.studio.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.data.local.SettingsDataStore
import com.aislide.studio.data.remote.LlmProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {

    val apiKey: StateFlow<String> = settings.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val providerId: StateFlow<String> = settings.providerId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LlmProvider.GLM.id)

    val endpoint: StateFlow<String> = settings.endpoint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LlmProvider.GLM.defaultEndpoint)

    val model: StateFlow<String> = settings.model
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LlmProvider.GLM.defaultModel)

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
}

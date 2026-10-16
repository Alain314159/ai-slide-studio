package com.aislide.studio.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.data.repository.LlmRepository
import com.aislide.studio.domain.usecase.GeneratePresentationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiGenerateViewModel @Inject constructor(
    private val generateUseCase: GeneratePresentationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<AiGenerateState>(AiGenerateState.Idle)
    val state: StateFlow<AiGenerateState> = _state.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    fun onPromptChange(value: String) {
        _prompt.value = value
    }

    fun generate() {
        val promptValue = _prompt.value.trim()
        if (promptValue.isBlank()) return
        viewModelScope.launch {
            _state.value = AiGenerateState.Loading
            generateUseCase(promptValue)
                .onSuccess { projectId -> _state.value = AiGenerateState.Success(projectId) }
                .onFailure { e ->
                    _state.value = AiGenerateState.Error(e.message ?: "Error desconocido")
                }
        }
    }

    fun reset() { _state.value = AiGenerateState.Idle }
}

sealed interface AiGenerateState {
    data object Idle : AiGenerateState
    data class Loading(val message: String = "La IA está creando tu presentación…") : AiGenerateState
    data class Success(val projectId: Long) : AiGenerateState
    data class Error(val message: String) : AiGenerateState
}

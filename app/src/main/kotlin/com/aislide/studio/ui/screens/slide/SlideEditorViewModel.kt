package com.aislide.studio.ui.screens.slide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.usecase.EditObjectWithAiUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val editObjectUseCase: EditObjectWithAiUseCase
) : ViewModel() {

    val projectId: Long = savedStateHandle["projectId"] ?: 0L
    val slideIndex: Int = savedStateHandle["slideIndex"] ?: 0

    private val _slide = MutableStateFlow<Slide?>(null)
    val slide: StateFlow<Slide?> = _slide.asStateFlow()

    private val _selectedObjectId = MutableStateFlow<String?>(null)
    val selectedObjectId: StateFlow<String?> = _selectedObjectId.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()

    init {
        loadSlide()
    }

    private fun loadSlide() {
        projectRepository.observeProject(projectId)
            .onEach { pres ->
                pres?.slides?.find { it.index == slideIndex }?.let { _slide.value = it }
            }
            .launchIn(viewModelScope)
    }

    fun selectObject(id: String?) { _selectedObjectId.value = id }

    fun editObjectWithAi(instruction: String) {
        val slide = _slide.value ?: return
        val objId = _selectedObjectId.value ?: return
        val target = slide.objects.find { it.id == objId } ?: return
        _aiLoading.value = true
        _aiError.value = null
        viewModelScope.launch {
            editObjectUseCase(projectId, slide, target, instruction)
                .onSuccess { explanation ->
                    _aiExplanation.value = explanation
                    // slide will reload via flow
                }
                .onFailure { e ->
                    _aiError.value = e.message ?: "Error desconocido"
                }
            _aiLoading.value = false
        }
    }

    fun clearExplanation() { _aiExplanation.value = null }
    fun clearError() { _aiError.value = null }
}

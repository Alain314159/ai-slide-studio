package com.aislide.studio.ui.screens.slide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.usecase.AddSlideObjectUseCase
import com.aislide.studio.domain.usecase.EditObjectWithAiUseCase
import com.aislide.studio.domain.usecase.GenerateImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val editObjectUseCase: EditObjectWithAiUseCase,
    private val addSlideObjectUseCase: AddSlideObjectUseCase,
    private val generateImageUseCase: GenerateImageUseCase
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

    private val _imageLoading = MutableStateFlow(false)
    val imageLoading: StateFlow<Boolean> = _imageLoading.asStateFlow()

    init {
        loadSlide()
    }

    private fun loadSlide() {
        projectRepository.observeProject(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
            .let { projectFlow ->
                viewModelScope.launch {
                    projectFlow.collect { pres ->
                        pres?.slides?.find { it.index == slideIndex }?.let { _slide.value = it }
                    }
                }
            }
    }

    fun selectObject(id: String?) { _selectedObjectId.value = id }

    val selectedObject: SlideObject?
        get() = _slide.value?.objects?.find { it.id == _selectedObjectId.value }

    fun editObjectWithAi(instruction: String) {
        val slide = _slide.value ?: return
        val objId = _selectedObjectId.value ?: return
        val target = slide.objects.find { it.id == objId } ?: return
        _aiLoading.value = true
        _aiError.value = null
        viewModelScope.launch {
            editObjectUseCase(projectId, slide, target, instruction)
                .onSuccess { explanation -> _aiExplanation.value = explanation }
                .onFailure { e -> _aiError.value = e.message ?: "Error desconocido" }
            _aiLoading.value = false
        }
    }

    fun addTextBox() {
        viewModelScope.launch {
            addSlideObjectUseCase(
                projectId, slideIndex,
                com.aislide.studio.data.model.ObjectType.TEXT_BOX,
                text = "Texto nuevo"
            )
        }
    }

    fun addBulletList() {
        viewModelScope.launch {
            addSlideObjectUseCase(
                projectId, slideIndex,
                com.aislide.studio.data.model.ObjectType.BULLET_LIST,
                bullets = listOf("Primer punto", "Segundo punto")
            )
        }
    }

    fun addImage(path: String) {
        viewModelScope.launch {
            addSlideObjectUseCase(
                projectId, slideIndex,
                com.aislide.studio.data.model.ObjectType.IMAGE,
                imageUrl = path
            )
        }
    }

    fun generateImageForPrompt(prompt: String) {
        _imageLoading.value = true
        _aiError.value = null
        viewModelScope.launch {
            generateImageUseCase(prompt, slide?.id ?: "")
                .onSuccess { path -> addImage(path) }
                .onFailure { e -> _aiError.value = e.message ?: "Error al generar imagen" }
            _imageLoading.value = false
        }
    }

    fun clearExplanation() { _aiExplanation.value = null }
    fun clearError() { _aiError.value = null }
}

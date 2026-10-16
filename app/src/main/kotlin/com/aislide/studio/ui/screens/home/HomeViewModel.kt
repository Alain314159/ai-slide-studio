package com.aislide.studio.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.usecase.ImportPptxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val importPptxUseCase: ImportPptxUseCase
) : ViewModel() {

    val projects: StateFlow<List<Presentation>> = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun importPptx(uri: android.net.Uri, title: String) {
        viewModelScope.launch {
            _importing.value = true
            _error.value = null
            importPptxUseCase(uri, title)
                .onSuccess { _importing.value = false }
                .onFailure { e ->
                    _error.value = e.message ?: "Error al importar"
                    _importing.value = false
                }
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch { projectRepository.delete(id) }
    }

    fun renameProject(id: Long, title: String) {
        viewModelScope.launch { projectRepository.rename(id, title) }
    }

    fun clearError() { _error.value = null }
}

package com.aislide.studio.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val projectId: Long = savedStateHandle["projectId"] ?: 0L

    val presentation: StateFlow<Presentation?> = projectRepository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun deleteSlide(index: Int) {
        viewModelScope.launch { projectRepository.deleteSlide(projectId, index) }
    }

    fun rename(title: String) {
        viewModelScope.launch { projectRepository.rename(projectId, title) }
    }
}

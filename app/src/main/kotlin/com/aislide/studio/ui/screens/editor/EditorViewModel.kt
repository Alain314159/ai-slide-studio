package com.aislide.studio.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.Typography
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer
) : ViewModel() {

    val projectId: Long = savedStateHandle["projectId"] ?: 0L

    val presentation: StateFlow<Presentation?> = projectRepository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Returns non-interactive HTML for the given slide, suitable for a thumbnail WebView.
     */
    fun renderThumbnail(slideIndex: Int): String {
        val pres = presentation.value ?: return ""
        val slide = pres.slides.getOrNull(slideIndex) ?: return ""
        return htmlRenderer.render(slide, pres.palette, pres.typography, interactive = false)
    }

    fun addSlide() {
        viewModelScope.launch {
            val pres = presentation.value ?: return@launch
            val newIndex = pres.slides.size
            val blankSlide = Slide(
                id = UUID.randomUUID().toString(),
                index = newIndex,
                layout = SlideLayout.TITLE_AND_BULLETS,
                title = "Nuevo slide",
                objects = listOf(
                    SlideObject(
                        id = UUID.randomUUID().toString(),
                        type = com.aislide.studio.data.model.ObjectType.TITLE,
                        text = "Título",
                        position = com.aislide.studio.data.model.ObjectPosition(
                            0.08f, 0.12f, 0.84f, 0.16f
                        ),
                        style = com.aislide.studio.data.model.ObjectStyle.default()
                            .copy(fontSizeSp = 40, fontWeight = 700)
                    )
                ),
                htmlContent = htmlRenderer.render(
                    Slide(
                        id = UUID.randomUUID().toString(),
                        index = newIndex,
                        layout = SlideLayout.TITLE_AND_BULLETS,
                        title = "Nuevo slide",
                        objects = listOf(
                            SlideObject(
                                id = UUID.randomUUID().toString(),
                                type = com.aislide.studio.data.model.ObjectType.TITLE,
                                text = "Título",
                                position = com.aislide.studio.data.model.ObjectPosition(
                                    0.08f, 0.12f, 0.84f, 0.16f
                                ),
                                style = com.aislide.studio.data.model.ObjectStyle.default()
                                    .copy(fontSizeSp = 40, fontWeight = 700)
                            )
                        )
                    ),
                    pres.palette,
                    pres.typography
                )
            )
            projectRepository.addSlide(projectId, blankSlide)
        }
    }

    fun deleteSlide(index: Int) {
        viewModelScope.launch { projectRepository.deleteSlide(projectId, index) }
    }

    fun moveSlide(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { projectRepository.moveSlide(projectId, fromIndex, toIndex) }
    }

    fun rename(title: String) {
        viewModelScope.launch { projectRepository.rename(projectId, title) }
    }
}

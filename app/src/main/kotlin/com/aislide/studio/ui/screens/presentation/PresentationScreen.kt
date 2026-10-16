package com.aislide.studio.ui.screens.presentation

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PresentationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer
) : ViewModel() {

    val projectId: Long = savedStateHandle["projectId"] ?: 0L

    val presentation: StateFlow<Presentation?> = projectRepository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun renderSlideHtml(slideIndex: Int): String? {
        val pres = presentation.value ?: return null
        val slide = pres.slides.getOrNull(slideIndex) ?: return null
        return htmlRenderer.render(slide, pres.palette, pres.typography)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationScreenRoute(
    navController: NavHostController,
    initialSlide: Int = 0,
    viewModel: PresentationViewModel = hiltViewModel()
) {
    val presentation by viewModel.presentation.collectAsState()
    var currentIndex by remember { mutableStateOf(initialSlide) }
    var swipeAccumulator by remember { mutableStateOf(0f) }

    LaunchedEffect(presentation) {
        if (currentIndex >= (presentation?.slides?.size ?: 0)) {
            currentIndex = 0
        }
    }

    val total = presentation?.slides?.size ?: 0
    val currentHtml = viewModel.renderSlideHtml(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slide ${currentIndex + 1}/$total") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Salir")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0
                ) { Icon(Icons.Default.NavigateBefore, contentDescription = "Anterior") }
                Text(
                    "${currentIndex + 1} / $total",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = {
                        if (currentIndex < total - 1) currentIndex++
                    },
                    enabled = currentIndex < total - 1
                ) { Icon(Icons.Default.NavigateNext, contentDescription = "Siguiente") }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(currentIndex) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Threshold-based swipe
                            if (swipeAccumulator > 80f && currentIndex < total - 1) currentIndex++
                            else if (swipeAccumulator < -80f && currentIndex > 0) currentIndex--
                            swipeAccumulator = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeAccumulator += dragAmount
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val html = currentHtml
            if (html != null) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        }
                    },
                    update = { web ->
                        web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Cargando…")
            }
        }
    }
}

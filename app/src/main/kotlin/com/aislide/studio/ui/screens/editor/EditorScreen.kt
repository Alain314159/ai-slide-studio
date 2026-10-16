package com.aislide.studio.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aislide.studio.ui.components.SlideThumbnail
import com.aislide.studio.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreenRoute(
    navController: NavHostController,
    projectId: Long,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val presentation by viewModel.presentation.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(presentation?.title ?: "Editor") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.presentation(projectId)) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Presentar")
                    }
                    IconButton(onClick = { navController.navigate(Routes.export(projectId)) }) {
                        Icon(Icons.Default.IosShare, contentDescription = "Exportar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addSlide() }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir slide")
            }
        }
    ) { padding ->
        val pres = presentation
        if (pres == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Cargando…")
            }
            return@Scaffold
        }
        if (pres.slides.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Esta presentación está vacía", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text("Toca + para añadir el primer slide", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pres.slides, key = { it.id }) { slide ->
                SlideCardRow(
                    slideIndex = slide.index,
                    slideTitle = slide.title.ifBlank { "Slide ${slide.index + 1}" },
                    thumbnailHtml = viewModel.renderThumbnail(slide.index),
                    totalSlides = pres.slides.size,
                    onClick = { navController.navigate(Routes.slideEditor(projectId, slide.index)) },
                    onDelete = { viewModel.deleteSlide(slide.index) },
                    onMoveUp = { viewModel.moveSlide(slide.index, slide.index - 1) },
                    onMoveDown = { viewModel.moveSlide(slide.index, slide.index + 1) }
                )
            }
        }
    }
}

@Composable
private fun SlideCardRow(
    slideIndex: Int,
    slideTitle: String,
    thumbnailHtml: String,
    totalSlides: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail (small WebView) — fixed width so the row doesn't reflow
            if (thumbnailHtml.isNotBlank()) {
                SlideThumbnail(
                    html = thumbnailHtml,
                    modifier = Modifier.size(width = 160.dp, height = 90.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${slideIndex + 1}", style = MaterialTheme.typography.titleMedium)
                Text(
                    slideTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = slideIndex > 0) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Mover arriba")
                }
                IconButton(onClick = onMoveDown, enabled = slideIndex < totalSlides - 1) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Mover abajo")
                }
            }
            IconButton(onClick = onClick) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
        }
    }
}

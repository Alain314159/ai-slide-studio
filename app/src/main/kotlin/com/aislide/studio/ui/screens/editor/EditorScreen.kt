package com.aislide.studio.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.aislide.studio.data.model.Presentation
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
                    IconButton(onClick = { navController.navigate(Routes.export(projectId)) }) {
                        Icon(Icons.Default.IosShare, contentDescription = "Exportar")
                    }
                }
            )
        },
        floatingActionButton = {
            IconButton(onClick = { /* TODO: add slide */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add slide")
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pres.slides, key = { it.id }) { slide ->
                SlideCardRow(
                    slideIndex = slide.index,
                    slideTitle = slide.title.ifBlank { "Slide ${slide.index + 1}" },
                    onClick = {
                        navController.navigate(Routes.slideEditor(projectId, slide.index))
                    },
                    onDelete = { viewModel.deleteSlide(slide.index) }
                )
            }
        }
    }
}

@Composable
private fun SlideCardRow(
    slideIndex: Int,
    slideTitle: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${slideIndex + 1}", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Text(slideTitle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onClick) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
        }
    }
}

@Composable
fun ExportScreenRoute(
    navController: NavHostController,
    projectId: Long,
    viewModel: EditorViewModel = hiltViewModel()
) {
    // Placeholder — full export UI in next iteration
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Exportar presentación", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Aquí se elegirá el modo de exportación (PPTX editable o PPTX visual SVG).")
        Text("Project ID: $projectId")
    }
}

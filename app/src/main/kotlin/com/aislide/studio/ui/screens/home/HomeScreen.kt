package com.aislide.studio.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aislide.studio.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenRoute(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val importing by viewModel.importing.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Slide Studio") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.AI_GENERATE) }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva presentación")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (projects.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { navController.navigate(Routes.editor(project.id)) },
                            onDelete = { viewModel.deleteProject(project.id) }
                        )
                    }
                }
            }
            if (importing) Text("Importando…", modifier = Modifier.padding(16.dp))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(64.dp))
        Text("Aún no tienes presentaciones.", style = MaterialTheme.typography.bodyLarge)
        Text("Toca + para crear una con IA", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProjectCard(
    project: Presentation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(project.title, style = MaterialTheme.typography.titleMedium)
            Text("${project.slideCount} slides", style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Add, contentDescription = "Eliminar") }
        }
    }
}

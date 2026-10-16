package com.aislide.studio.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aislide.studio.data.model.Presentation
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

    var showRenameDialog by remember { mutableStateOf<Long?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var importTitle by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val filteredProjects by remember(projects, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) projects
            else projects.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val pickPptxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importUri = uri
            importTitle = "Presentación importada"
            showRenameDialog = -1L
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Buscar…") }
                        )
                    } else {
                        Text("AI Slide Studio")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                    IconButton(onClick = {
                        pickPptxLauncher.launch(arrayOf(
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                        ))
                    }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Importar PPTX")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.AI_GENERATE) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva con IA") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filteredProjects.isEmpty() && !importing && searchQuery.isBlank()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else if (filteredProjects.isEmpty() && searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin resultados para \"$searchQuery\"", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (importing) {
                        item { Text("Importando .pptx…", style = MaterialTheme.typography.bodyMedium) }
                    }
                    items(filteredProjects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { navController.navigate(Routes.editor(project.id)) },
                            onDelete = { viewModel.deleteProject(project.id) },
                            onRename = { showRenameDialog = project.id }
                        )
                    }
                }
            }
            error?.let { err ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::clearError) { Text("OK") }
                    }
                }
            }
        }
    }

    val dialogId = showRenameDialog
    if (dialogId != null) {
        val isImport = dialogId == -1L
        if (isImport) {
            AlertDialog(
                onDismissRequest = {
                    showRenameDialog = null
                    importUri = null
                    importTitle = ""
                },
                title = { Text("Importar .pptx") },
                text = {
                    OutlinedTextField(
                        value = importTitle,
                        onValueChange = { importTitle = it },
                        label = { Text("Nombre de la presentación") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val uri = importUri
                        val title = importTitle.ifBlank { "Importado" }
                        if (uri != null) viewModel.importPptx(uri, title)
                        showRenameDialog = null
                        importUri = null
                        importTitle = ""
                    }) { Text("Importar") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRenameDialog = null
                        importUri = null
                        importTitle = ""
                    }) { Text("Cancelar") }
                }
            )
        } else {
            var renameValue by remember { mutableStateOf(projects.find { it.id == dialogId }?.title ?: "") }
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text("Renombrar") },
                text = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.renameProject(dialogId, renameValue)
                        showRenameDialog = null
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(72.dp))
        Text("Aún no tienes presentaciones", style = MaterialTheme.typography.titleMedium)
        Text("Toca \"Nueva con IA\" o importa un .pptx existente", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProjectCard(
    project: Presentation,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(project.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${project.slideCount} slides • ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(project.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Renombrar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

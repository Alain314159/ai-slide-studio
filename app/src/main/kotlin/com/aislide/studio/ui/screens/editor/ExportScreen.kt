package com.aislide.studio.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.domain.usecase.ExportToPptxUseCase
import com.aislide.studio.domain.usecase.ExportToPptxVectorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExportMode(val label: String, val description: String) {
    EDITABLE("PPTX editable", "Texto seleccionable en PowerPoint. Ideal si el profe va a editar."),
    VISUAL("PPTX visual (alta calidad)", "Imagen de alta resolución por slide. Se ve exactamente como en la app.")
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportEditable: ExportToPptxUseCase,
    private val exportVisual: ExportToPptxVectorUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    fun export(projectId: Long, mode: ExportMode, outputUri: Uri, fileName: String) {
        viewModelScope.launch {
            _state.value = ExportState.Loading
            val result = when (mode) {
                ExportMode.EDITABLE -> exportEditable(projectId, outputUri, fileName)
                ExportMode.VISUAL -> exportVisual(projectId, outputUri, fileName)
            }
            _state.value = result.fold(
                onSuccess = { bytes -> ExportState.Success(bytes) },
                onFailure = { e -> ExportState.Error(e.message ?: "Error desconocido") }
            )
        }
    }

    fun reset() { _state.value = ExportState.Idle }
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Loading : ExportState
    data class Success(val sizeBytes: Long) : ExportState
    data class Error(val message: String) : ExportState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreenRoute(
    navController: NavHostController,
    projectId: Long,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedMode by remember { mutableStateOf(ExportMode.EDITABLE) }
    var fileName by remember { mutableStateOf("presentacion") }
    var selectedTreeUri by remember { mutableStateOf<Uri?>(null) }

    val pickDirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            selectedTreeUri = uri
            // Persist permission so we can write later
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            navController.context.applicationContext.contentResolver.takePersistableUriPermission(uri, flags)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportar presentación") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Modo de exportación", style = MaterialTheme.typography.titleMedium)
            ExportMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (selectedMode == mode),
                            onClick = { selectedMode = mode }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedMode == mode),
                        onClick = { selectedMode = mode }
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Column {
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                        Text(mode.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Nombre del archivo", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = { Text(".pptx") }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Carpeta de destino", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { pickDirLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedTreeUri?.let { "Carpeta seleccionada" } ?: "Elegir carpeta")
            }
            selectedTreeUri?.let {
                Text("✓ Lista para guardar", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val uri = selectedTreeUri
                    val name = fileName.ifBlank { "presentacion" }
                    if (uri != null) {
                        viewModel.export(projectId, selectedMode, uri, name)
                    }
                },
                enabled = selectedTreeUri != null && fileName.isNotBlank() && state !is ExportState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Exportar presentación")
            }

            when (val s = state) {
                ExportState.Idle -> Unit
                ExportState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Generando .pptx…")
                        }
                    }
                }
                is ExportState.Success -> {
                    Text(
                        "✓ Exportado correctamente (${s.sizeBytes / 1024} KB)",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(8.dp)
                    )
                    Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Hecho")
                    }
                }
                is ExportState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                    Button(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

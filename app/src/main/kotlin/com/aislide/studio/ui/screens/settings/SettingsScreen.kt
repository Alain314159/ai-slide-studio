package com.aislide.studio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aislide.studio.data.remote.LlmProvider
import com.aislide.studio.domain.usecase.ValidationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenRoute(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val providerId by viewModel.providerId.collectAsState()
    val endpoint by viewModel.endpoint.collectAsState()
    val model by viewModel.model.collectAsState()
    val validating by viewModel.validating.collectAsState()
    val validation by viewModel.validation.collectAsState()
    var showKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Proveedor de IA", style = MaterialTheme.typography.titleMedium)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = LlmProvider.fromId(providerId).displayName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    LlmProvider.entries.forEach { p ->
                        DropdownMenuItem(text = { Text(p.displayName) }, onClick = {
                            viewModel.setProviderId(p.id)
                            if (p != LlmProvider.CUSTOM) {
                                viewModel.setEndpoint(p.defaultEndpoint)
                                viewModel.setModel(p.defaultModel)
                            }
                            viewModel.clearValidation()
                            expanded = false
                        })
                    }
                }
            }

            Text("API Key", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = apiKey,
                onValueChange = { viewModel.setApiKey(it); viewModel.clearValidation() },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "🙈" else "👁")
                    }
                }
            )

            Text("Endpoint", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = endpoint,
                onValueChange = { viewModel.setEndpoint(it); viewModel.clearValidation() },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Modelo", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = model,
                onValueChange = { viewModel.setModel(it); viewModel.clearValidation() },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Validate button + result
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = viewModel::validate,
                enabled = apiKey.isNotBlank() && !validating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Probando…")
                } else {
                    Text("Probar conexión")
                }
            }
            validation?.let { result ->
                when (result) {
                    is ValidationResult.Success -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            "✓ Conexión exitosa. Respuesta: ${result.reply}",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is ValidationResult.InvalidKey -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            "API key inválida (401/403). Revisa tu key.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is ValidationResult.WrongEndpoint -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            "Endpoint no encontrado (404). Revisa la URL.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is ValidationResult.Error -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            "Error: ${result.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Acerca de", style = MaterialTheme.typography.titleMedium)
            Text(
                "AI Slide Studio v0.3.0\nCreador de presentaciones con IA\nUso personal\n\nConfigura tu proveedor de IA (GLM por defecto), pega tu API key, prueba la conexión, y vuelve atrás para crear tu primera presentación con IA.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

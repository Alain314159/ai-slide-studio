package com.aislide.studio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aislide.studio.data.remote.LlmProvider

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
                            expanded = false
                        })
                    }
                }
            }

            Text("API Key", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = apiKey,
                onValueChange = viewModel::setApiKey,
                modifier = Modifier.fillMaxWidth(),
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
                onValueChange = viewModel::setEndpoint,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Modelo", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = model,
                onValueChange = viewModel::setModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

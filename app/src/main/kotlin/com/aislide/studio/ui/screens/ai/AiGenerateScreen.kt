package com.aislide.studio.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aislide.studio.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenerateScreenRoute(
    navController: NavHostController,
    viewModel: AiGenerateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val prompt by viewModel.prompt.collectAsState()

    LaunchedEffect(state) {
        if (state is AiGenerateState.Success) {
            val projectId = (state as AiGenerateState.Success).projectId
            navController.navigate(Routes.editor(projectId)) {
                popUpTo(Routes.HOME) { inclusive = false }
            }
            viewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear con IA") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                AiGenerateState.Idle -> IdleView(
                    prompt = prompt,
                    onPromptChange = viewModel::onPromptChange,
                    onGenerate = viewModel::generate,
                    modifier = Modifier.fillMaxSize()
                )
                is AiGenerateState.Loading -> LoadingView(s.message, Modifier.fillMaxSize())
                is AiGenerateState.Error -> ErrorView(
                    message = s.message,
                    onRetry = viewModel::generate,
                    onReset = viewModel::reset,
                    modifier = Modifier.fillMaxSize()
                )
                is AiGenerateState.Success -> Unit  // navigated away
            }
        }
    }
}

@Composable
private fun IdleView(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Describe la presentación que necesitas",
            style = MaterialTheme.typography.headlineSmall
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            placeholder = {
                Text("Ej: Presentación sobre la fotosíntesis para 7mo grado, 8 slides, tono educativo con ejemplos de la vida real")
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onGenerate,
            enabled = prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Generar presentación") }
    }
}

@Composable
private fun LoadingView(message: String, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Error", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Reintentar") }
        Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}

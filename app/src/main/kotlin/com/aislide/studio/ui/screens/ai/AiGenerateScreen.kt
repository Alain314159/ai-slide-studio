package com.aislide.studio.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aislide.studio.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenerateScreenRoute(
    navController: NavHostController,
    templateId: String?,
    viewModel: AiGenerateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val template = viewModel.template

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
                    templateName = template?.name,
                    templateEmoji = template?.previewEmoji,
                    templatePalette = template?.palette,
                    modifier = Modifier.fillMaxSize()
                )
                is AiGenerateState.Loading -> LoadingView(s.message, Modifier.fillMaxSize())
                is AiGenerateState.Error -> ErrorView(
                    message = s.message,
                    onRetry = viewModel::generate,
                    onReset = viewModel::reset,
                    modifier = Modifier.fillMaxSize()
                )
                is AiGenerateState.Success -> Unit
            }
        }
    }
}

@Composable
private fun IdleView(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    templateName: String?,
    templateEmoji: String?,
    templatePalette: com.aislide.studio.data.model.Palette?,
    modifier: Modifier
) {
    val examplePrompts: List<String> = remember {
        listOf(
            "Presentación sobre la fotosíntesis para 7mo grado, 8 slides, tono educativo con ejemplos de la vida real",
            "Resumen de la Revolución Francesa, 10 slides, con cronología y causas principales",
            "Introducción a la programación orientada a objetos, 12 slides, con ejemplos en Python",
            "Beneficios de la actividad física, 6 slides, tono motivador, con datos científicos",
            "Cultura maya: historia, matemáticas y astronomía, 10 slides",
            "Energías renovables: ventajas y desafíos, 8 slides, con gráficos comparativos",
            "Tutorial: cómo hacer una buena exposición oral, 7 slides",
            "Impacto de las redes sociales en los jóvenes, 9 slides, tono reflexivo"
        )
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Describe la presentación que necesitas",
            style = MaterialTheme.typography.headlineSmall
        )

        // Show selected template banner (if any)
        if (templateName != null && templatePalette != null) {
            TemplateBanner(
                name = templateName,
                emoji = templateEmoji ?: "🎨",
                palette = templatePalette,
                onRemove = { onPromptChange(prompt) }  // keep prompt text, just visual
            )
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            placeholder = {
                Text("Ej: Presentación sobre la fotosíntesis para 7mo grado, 8 slides, tono educativo con ejemplos de la vida real")
            }
        )

        Text("O elige un ejemplo:", style = MaterialTheme.typography.bodyMedium)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = examplePrompts) { example: String ->
                AssistChip(
                    onClick = { onPromptChange(example) },
                    label = { Text(example.take(40) + if (example.length > 40) "…" else "", style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(16.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onGenerate,
            enabled = prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Generar presentación")
        }
    }
}

@Composable
private fun TemplateBanner(
    name: String,
    emoji: String,
    palette: com.aislide.studio.data.model.Palette,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Plantilla seleccionada", style = MaterialTheme.typography.labelSmall)
                Text(name, style = MaterialTheme.typography.titleSmall)
            }
            // Palette swatches
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(palette.primary, palette.secondary, palette.accent, palette.background).forEach { hex ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
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

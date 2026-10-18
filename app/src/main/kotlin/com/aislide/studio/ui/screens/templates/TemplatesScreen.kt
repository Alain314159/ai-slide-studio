package com.aislide.studio.ui.screens.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aislide.studio.data.model.PresentationTemplate
import com.aislide.studio.data.model.TemplateCategory
import com.aislide.studio.data.model.PresentationTemplates

/**
 * Templates screen — Canva-style picker.
 *
 * Shows all available templates grouped by category. User taps a template
 * to use it → navigates to AI Generate screen with the template pre-selected
 * (its palette, typography, and prompt hint will guide the LLM).
 *
 * Templates are 100% local (no internet needed) — all from open-source
 * color palettes and Google Fonts (OFL licensed).
 *
 * Categories:
 * - Educativo: 2 templates (vibrante + limpio)
 * - Corporativo: 2 (azul + elegante)
 * - Creativo: 2 (audaz + cálido)
 * - Minimalista: 2 (monocromo + pastel)
 * - Oscuro: 2 (tech + premium)
 * - Vivo: 2 (pop art + tropical)
 * - Académico: 2 (clásico + moderno)
 * - Startup: 2 (pitch + YC minimal)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreenRoute(
    navController: NavHostController,
    onTemplateSelected: (PresentationTemplate) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }

    val filteredTemplates = remember(searchQuery, selectedCategory) {
        PresentationTemplates.all.filter { template ->
            val matchesSearch = searchQuery.isBlank() ||
                template.name.contains(searchQuery, ignoreCase = true) ||
                template.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || template.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plantillas") },
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
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Buscar plantillas…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            // Category chips (horizontal scroll)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todas") }
                    )
                }
                items(TemplateCategory.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        label = { Text(categoryLabel(category)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Templates list
            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin resultados para \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTemplates, key = { it.id }) { template ->
                        TemplateCard(template = template, onClick = { onTemplateSelected(template) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: PresentationTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: emoji + name + category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    template.previewEmoji,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.size(48.dp).padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        categoryLabel(template.category),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Color palette preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ColorSwatch(template.palette.primary)
                ColorSwatch(template.palette.secondary)
                ColorSwatch(template.palette.accent)
                ColorSwatch(template.palette.background)
                ColorSwatch(template.palette.surface)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Typography preview
            Text(
                "${template.typography.headingFont} + ${template.typography.bodyFont}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorSwatch(hex: String) {
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
    )
}

private fun categoryLabel(category: TemplateCategory): String = when (category) {
    TemplateCategory.EDUCATIVO -> "Educativo"
    TemplateCategory.CORPORATIVO -> "Corporativo"
    TemplateCategory.CREATIVO -> "Creativo"
    TemplateCategory.MINIMALISTA -> "Minimalista"
    TemplateCategory.OSCURO -> "Oscuro"
    TemplateCategory.VIVO -> "Vivo"
    TemplateCategory.ACADEMICO -> "Académico"
    TemplateCategory.STARTUP -> "Startup"
}

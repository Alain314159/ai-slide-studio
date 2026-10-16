package com.aislide.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.TextAlign

/**
 * Bottom sheet for editing a selected object's style:
 * - Font size (slider 8-96)
 * - Font weight (chips: 300/400/500/600/700/800)
 * - Text color (preset palette)
 * - Text align (4 chips)
 * - Background color (toggle + presets)
 * - Border radius (slider 0-32)
 * - Shadow (switch)
 *
 * Applies changes live via [onApply] which calls [com.aislide.studio.domain.usecase.UpdateObjectStyleUseCase].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleEditorSheet(
    initialStyle: ObjectStyle,
    onDismiss: () -> Unit,
    onApply: (ObjectStyle) -> Unit
) {
    var fontSize by remember { mutableStateOf(initialStyle.fontSizeSp.toFloat()) }
    var fontWeight by remember { mutableStateOf(initialStyle.fontWeight) }
    var textColor by remember { mutableStateOf(initialStyle.textColorHex) }
    var textAlign by remember { mutableStateOf(initialStyle.textAlign) }
    var useBg by remember { mutableStateOf(initialStyle.backgroundColorHex != null) }
    var bgColor by remember { mutableStateOf(initialStyle.backgroundColorHex ?: "#000000") }
    var borderRadius by remember { mutableStateOf(initialStyle.borderRadiusDp.toFloat()) }
    var hasShadow by remember { mutableStateOf(initialStyle.hasShadow) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Estilo del objeto", style = MaterialTheme.typography.titleLarge)

            // Font size
            Text("Tamaño: ${fontSize.toInt()}sp", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                valueRange = 8f..96f,
                steps = 0
            )

            // Font weight
            Text("Peso", style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(300 to "Light", 400 to "Regular", 500 to "Medium", 600 to "Semi", 700 to "Bold", 800 to "Extra").forEach { (weight, label) ->
                    FilterChip(
                        selected = fontWeight == weight,
                        onClick = { fontWeight = weight },
                        label = { Text(label) }
                    )
                }
            }

            // Text align
            Text("Alineación", style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextAlign.entries.forEach { align ->
                    FilterChip(
                        selected = textAlign == align,
                        onClick = { textAlign = align },
                        label = {
                            Text(
                                when (align) {
                                    TextAlign.LEFT -> "Izq"
                                    TextAlign.CENTER -> "Centro"
                                    TextAlign.RIGHT -> "Der"
                                    TextAlign.JUSTIFIED -> "Just"
                                }
                            )
                        }
                    )
                }
            }

            // Text color
            Text("Color del texto", style = MaterialTheme.typography.bodyMedium)
            ColorRow(
                selected = textColor,
                onSelected = { textColor = it }
            )
            HexTextField(
                hex = textColor,
                onHexChange = { textColor = it },
                label = "HEX del texto"
            )

            // Background color (toggle + picker)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = useBg, onCheckedChange = {
                    useBg = it
                    if (!it) bgColor = ""
                })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Color de fondo", style = MaterialTheme.typography.bodyMedium)
            }
            if (useBg) {
                ColorRow(
                    selected = bgColor,
                    onSelected = { bgColor = it }
                )
                HexTextField(
                    hex = bgColor,
                    onHexChange = { bgColor = it },
                    label = "HEX del fondo"
                )
            }

            // Border radius
            Text("Esquinas redondeadas: ${borderRadius.toInt()}dp", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = borderRadius,
                onValueChange = { borderRadius = it },
                valueRange = 0f..32f
            )

            // Shadow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = hasShadow, onCheckedChange = { hasShadow = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sombra", style = MaterialTheme.typography.bodyMedium)
            }

            // Apply
            Button(
                onClick = {
                    val newStyle = initialStyle.copy(
                        fontSizeSp = fontSize.toInt(),
                        fontWeight = fontWeight,
                        textColorHex = textColor,
                        textAlign = textAlign,
                        backgroundColorHex = if (useBg) bgColor else null,
                        borderRadiusDp = borderRadius.toInt(),
                        hasShadow = hasShadow
                    )
                    onApply(newStyle)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Aplicar") }
        }
    }
}

@Composable
private fun ColorRow(
    selected: String,
    onSelected: (String) -> Unit
) {
    val colors = listOf(
        "#FFFFFF" to "Blanco",
        "#000000" to "Negro",
        "#FF5252" to "Rojo",
        "#FFC857" to "Amarillo",
        "#6BCB7A" to "Verde",
        "#5B6CFF" to "Azul",
        "#7DD3C0" to "Cyan",
        "#B47BFF" to "Morado",
        "#FF9BC0" to "Rosa",
        "#FFA500" to "Naranja"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { (hex, _) ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(hex)))
                    .clickable { onSelected(hex) }
            ) {
                if (selected.equals(hex, ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

/**
 * Text field for entering a custom HEX color (e.g. "#FF5722").
 * Validates as you type; on blur, normalizes to #RRGGBB.
 */
@Composable
private fun HexTextField(
    hex: String,
    onHexChange: (String) -> Unit,
    label: String
) {
    var text by remember(hex) { mutableStateOf(hex.removePrefix("#")) }
    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            // Filter to hex chars only (0-9, A-F after uppercase), max 6 chars
            val filtered = newValue.uppercase().filter { c -> c.isDigit() || c in 'A'..'F' }.take(6)
            text = filtered
            if (filtered.length == 6 || filtered.length == 3) {
                onHexChange("#$filtered")
            } else if (filtered.isEmpty()) {
                onHexChange("#000000")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        prefix = { Text("#") },
        isError = text.length != 6 && text.length != 3
    )
}

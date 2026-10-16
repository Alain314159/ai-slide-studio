package com.aislide.studio.ui.screens.slide

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.ObjectSelectionBridge

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideEditorScreenRoute(
    navController: NavHostController,
    projectId: Long,
    slideIndex: Int,
    viewModel: SlideEditorViewModel = hiltViewModel()
) {
    val slide by viewModel.slide.collectAsState()
    val selectedObjId by viewModel.selectedObjectId.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiError by viewModel.aiError.collectAsState()
    val aiExplanation by viewModel.aiExplanation.collectAsState()
    val imageLoading by viewModel.imageLoading.collectAsState()

    var showAiSheet by remember { mutableStateOf(false) }
    var showImageSheet by remember { mutableStateOf(false) }
    var aiInstruction by remember { mutableStateOf("") }
    var imagePrompt by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slide ${slideIndex + 1}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = viewModel.canUndo) {
                        Icon(Icons.Default.Undo, contentDescription = "Deshacer")
                    }
                    IconButton(onClick = viewModel::redo, enabled = viewModel.canRedo) {
                        Icon(Icons.Default.Redo, contentDescription = "Rehacer")
                    }
                }
            )
        }
    ) { padding ->
        val currentSlide = slide
        if (currentSlide == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // === Slide canvas (left, 70%) ===
            Column(modifier = Modifier.weight(0.7f).padding(8.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true  // required for tap-to-select
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                // Bridge that JS calls back when an object is tapped
                                addJavascriptInterface(
                                    ObjectSelectionBridge { id ->
                                        viewModel.selectObject(id.ifBlank { null })
                                    },
                                    HtmlSlideRenderer.JS_BRIDGE_NAME
                                )
                                webViewClient = WebViewClient()
                                // Initial load is set in update {}
                            }.also { webViewRef = it }
                        },
                        update = { web ->
                            // Re-render with interactive=true so JS is injected
                            val pres = viewModel.presentation.value
                            if (pres != null) {
                                val html = viewModel.renderInteractiveSlide(currentSlide.index)
                                web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                // After page loads, sync the selection from ViewModel
                                web.post {
                                    val selId = selectedObjId
                                    val js = if (selId.isNullOrBlank()) {
                                        "window.__setSelection && window.__setSelection('');"
                                    } else {
                                        "window.__setSelection && window.__setSelection('$selId');"
                                    }
                                    web.evaluateJavascript(js, null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = viewModel::addTextBox, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.TextFields, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Texto")
                    }
                    Button(onClick = viewModel::addBulletList, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bullets")
                    }
                    Button(onClick = { showImageSheet = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Imagen IA")
                    }
                }
            }

            // === Object list (right, 30%) — for visual feedback and AI access ===
            Column(
                modifier = Modifier.weight(0.3f).fillMaxHeight().padding(8.dp)
            ) {
                Text("Objetos", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (currentSlide.objects.isEmpty()) {
                    Text(
                        "No hay objetos. Añade uno con los botones de abajo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(currentSlide.objects, key = { it.id }) { obj ->
                            ObjectListItem(
                                obj = obj,
                                isSelected = obj.id == selectedObjId,
                                onClick = { viewModel.selectObject(obj.id) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedObjId != null) {
                    Button(
                        onClick = { showAiSheet = true },
                        enabled = !aiLoading && !imageLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar con IA")
                    }
                    IconButton(onClick = { viewModel.selectObject(null) }, modifier = Modifier.align(Alignment.End)) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar selección")
                    }
                }
                if (aiLoading) Text("IA trabajando…", modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.bodySmall)
                if (imageLoading) Text("Generando imagen…", modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.bodySmall)
                aiError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.bodySmall)
                }
                aiExplanation?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }

    // === AI Edit bottom sheet ===
    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("¿Qué quieres cambiar?", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = aiInstruction,
                    onValueChange = { aiInstruction = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Ej: hazlo más simple, añade un ejemplo, cambia el tono a académico") }
                )
                Button(
                    onClick = {
                        viewModel.editObjectWithAi(aiInstruction)
                        showAiSheet = false
                        aiInstruction = ""
                    },
                    enabled = aiInstruction.isNotBlank() && !aiLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Aplicar") }
            }
        }
    }

    // === Image generation bottom sheet ===
    if (showImageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Generar imagen con IA", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = imagePrompt,
                    onValueChange = { imagePrompt = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Ej: una planta verde con flores, estilo ilustración educativa") }
                )
                Button(
                    onClick = {
                        viewModel.generateImageForPrompt(imagePrompt)
                        showImageSheet = false
                        imagePrompt = ""
                    },
                    enabled = imagePrompt.isNotBlank() && !imageLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generar")
                }
            }
        }
    }
}

@Composable
private fun ObjectListItem(
    obj: SlideObject,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                when (obj.type) {
                    com.aislide.studio.data.model.ObjectType.TITLE -> "Título"
                    com.aislide.studio.data.model.ObjectType.SUBTITLE -> "Subtítulo"
                    com.aislide.studio.data.model.ObjectType.TEXT_BOX -> "Caja de texto"
                    com.aislide.studio.data.model.ObjectType.BULLET_LIST -> "Lista de bullets"
                    com.aislide.studio.data.model.ObjectType.IMAGE -> "Imagen"
                    com.aislide.studio.data.model.ObjectType.SHAPE_RECTANGLE -> "Rectángulo"
                    com.aislide.studio.data.model.ObjectType.SHAPE_CIRCLE -> "Círculo"
                    com.aislide.studio.data.model.ObjectType.SHAPE_ARROW -> "Flecha"
                    com.aislide.studio.data.model.ObjectType.SHAPE_LINE -> "Línea"
                    else -> obj.type.name
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                when {
                    obj.text.isNotBlank() -> obj.text.take(40)
                    obj.bullets.isNotEmpty() -> obj.bullets.firstOrNull().orEmpty().take(40)
                    obj.imageUrl != null -> "Imagen local"
                    else -> "(vacío)"
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
    }
}

package com.aislide.studio.ui.screens.slide

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

@SuppressLint("SetJavaScriptEnabled")
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

    var showAiSheet by remember { mutableStateOf(false) }
    var aiInstruction by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slide ${slideIndex + 1}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Slide rendered via WebView — HTML from the Slide.htmlContent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(8.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL(null, currentSlide.htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    update = { web -> web.loadDataWithBaseURL(null, currentSlide.htmlContent, "text/html", "UTF-8", null) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { showAiSheet = true }, enabled = selectedObjId != null && !aiLoading) {
                    Text("Editar objeto con IA")
                }
                if (selectedObjId != null) {
                    IconButton(onClick = { viewModel.selectObject(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Deselect")
                    }
                }
            }

            if (aiLoading) Text("IA trabajando…", modifier = Modifier.padding(8.dp))
            aiError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }
            aiExplanation?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
            }
        }
    }

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
                    placeholder = { Text("Ej: hazlo más simple, añade un ejemplo, cambia el tono") }
                )
                Button(
                    onClick = {
                        viewModel.editObjectWithAi(aiInstruction)
                        showAiSheet = false
                        aiInstruction = ""
                    },
                    enabled = aiInstruction.isNotBlank() && !aiLoading
                ) { Text("Aplicar") }
            }
        }
    }
}

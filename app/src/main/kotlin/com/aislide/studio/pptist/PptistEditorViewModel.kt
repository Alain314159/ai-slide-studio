package com.aislide.studio.pptist

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aislide.studio.llm.LlmService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel that owns the bridge state and orchestrates calls between
 * PPTist (in the WebView) and our Kotlin services (LLM, persistence).
 *
 * The ViewModel does NOT hold a direct reference to the WebView because
 * that would leak the Activity context. Instead, the composable passes
 * the WebView ref in when invoking handlers.
 */
@HiltViewModel
class PptistEditorViewModel @Inject constructor(
    private val llmService: LlmService
) : ViewModel() {

    private val _isEditorReady = MutableStateFlow(false)
    val isEditorReady: StateFlow<Boolean> = _isEditorReady.asStateFlow()

    private val _lastBridgeEvent = MutableStateFlow<PptistBridgeEvent?>(null)
    val lastBridgeEvent: StateFlow<PptistBridgeEvent?> = _lastBridgeEvent.asStateFlow()

    private val _lastSavedState = MutableStateFlow<String?>(null)
    val lastSavedState: StateFlow<String?> = _lastSavedState.asStateFlow()

    /**
     * Called by the WebView when PPTist emits a bridge event.
     * Stores the event so the composable can react on the main thread.
     */
    fun onBridgeEvent(event: PptistBridgeEvent) {
        _lastBridgeEvent.value = event
    }

    /**
     * React to a bridge event.
     * Called from the composable with the WebView reference so we can
     * call back into JS if needed.
     */
    fun handleBridgeEvent(event: PptistBridgeEvent, webView: WebView?) {
        when (event) {
            is PptistBridgeEvent.Ready -> {
                _isEditorReady.value = true
                Timber.d("Editor ready")
            }
            is PptistBridgeEvent.StateChanged -> {
                _lastSavedState.value = event.jsonState
                // TODO: persist to Room
            }
            is PptistBridgeEvent.SlideChanged -> {
                Timber.d("Slide changed: ${event.slideIndex}")
            }
            is PptistBridgeEvent.SelectionChanged -> {
                Timber.d("Selection: ${event.objectId}")
            }
            is PptistBridgeEvent.ExportRequest -> {
                Timber.d("Export: ${event.format} (${event.base64Data.length} bytes)")
                // TODO: save to SAF, share, etc.
            }
            is PptistBridgeEvent.AIRequest -> {
                handleAIRequest(event, webView)
            }
            is PptistBridgeEvent.Error -> {
                Timber.e("PPTist error: ${event.message}")
            }
        }
    }

    /**
     * Process an AI request from PPTist.
     * Calls our LLM and injects the result back into PPTist.
     */
    private fun handleAIRequest(event: PptistBridgeEvent.AIRequest, webView: WebView?) {
        if (webView == null) {
            Timber.w("WebView null, cannot process AI request")
            return
        }
        viewModelScope.launch {
            try {
                val response = when (event.action) {
                    "generate" -> llmService.generatePresentation(event.payload)
                    "edit_object" -> llmService.editObject(event.payload)
                    "edit_slide" -> llmService.editSlide(event.payload)
                    "improve_design" -> llmService.improveDesign(event.payload)
                    else -> {
                        Timber.w("Unknown AI action: ${event.action}")
                        return@launch
                    }
                }
                // Inject the response back into PPTist
                val js = "window.dispatchEvent(new CustomEvent('ai-response', { detail: " +
                    "{ action: '${event.action}', response: $response } }))"
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                Timber.e(e, "AI request failed")
                val js = "window.dispatchEvent(new CustomEvent('ai-error', { detail: " +
                    "{ action: '${event.action}', message: '${e.message}' } }))"
                webView.evaluateJavascript(js, null)
            }
        }
    }
}

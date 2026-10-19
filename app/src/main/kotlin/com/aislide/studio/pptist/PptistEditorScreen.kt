package com.aislide.studio.pptist

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import timber.log.Timber

/**
 * Screen that hosts the PPTist editor in a WebView and wires up the
 * JavaScript Bridge so the editor can talk to native Kotlin code.
 *
 * Page lifecycle:
 * - First load: WebView loads `file:///android_asset/pptist/index.html`
 * - PPTist boots in Vue, calls `AndroidBridge.onReady()` to signal it's loaded
 * - Kotlin can then invoke JS like `window.editor.loadJSON(state)`
 * - User actions emit events via `AndroidBridge.on{Event}(...)`
 *
 * The WebView is configured for offline use: no network access needed,
 * all assets served from app/src/main/assets/pptist/.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PptistEditorScreen(
    viewModel: PptistEditorViewModel = hiltViewModel()
) {
    val isReady by viewModel.isEditorReady.collectAsState()
    val lastEvent by viewModel.lastBridgeEvent.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // React to bridge events
    LaunchedEffect(lastEvent) {
        lastEvent?.let { event ->
            Timber.d("Bridge event: ${event::class.simpleName}")
            viewModel.handleBridgeEvent(event, webViewRef)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                createPptistWebView(ctx) { event ->
                    viewModel.onBridgeEvent(event)
                }.also { webViewRef = it }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Creates the WebView with all the configuration needed to host PPTist:
 * - JS enabled (required)
 * - DOM storage enabled (PPTist uses localStorage)
 * - File access for local assets
 * - Bridge interface exposed as `window.AndroidBridge`
 * - WebViewClient to handle in-app navigation (don't open external browser)
 * - WebChromeClient to handle console.log, alerts, etc.
 *
 * SECURITY NOTE: the @JavascriptInterface is intentionally minimal — only
 * events that PPTist emits. PPTist can call `AndroidBridge.onReady()`,
 * `AndroidBridge.onStateChange(...)`, etc. but cannot invoke arbitrary
 * Kotlin methods.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createPptistWebView(
    context: Context,
    onBridgeEvent: (PptistBridgeEvent) -> Unit
): WebView {
    return WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }

        // Bridge interface — PPTist calls these methods via window.AndroidBridge.*
        addJavascriptInterface(
            object {
                @JavascriptInterface
                fun onReady() {
                    Timber.d("PPTist: onReady")
                    onBridgeEvent(PptistBridgeEvent.Ready)
                }

                @JavascriptInterface
                fun onStateChange(jsonState: String) {
                    Timber.d("PPTist: onStateChange (${jsonState.length} chars)")
                    onBridgeEvent(PptistBridgeEvent.StateChanged(jsonState))
                }

                @JavascriptInterface
                fun onSlideChanged(slideIndex: Int) {
                    Timber.d("PPTist: onSlideChanged($slideIndex)")
                    onBridgeEvent(PptistBridgeEvent.SlideChanged(slideIndex))
                }

                @JavascriptInterface
                fun onSelectionChanged(objectId: String?) {
                    Timber.d("PPTist: onSelectionChanged($objectId)")
                    onBridgeEvent(PptistBridgeEvent.SelectionChanged(objectId))
                }

                @JavascriptInterface
                fun onExportRequest(format: String, base64Data: String) {
                    Timber.d("PPTist: onExportRequest($format, ${base64Data.length} chars)")
                    onBridgeEvent(PptistBridgeEvent.ExportRequest(format, base64Data))
                }

                @JavascriptInterface
                fun onAIRequest(action: String, payload: String) {
                    Timber.d("PPTist: onAIRequest($action, $payload)")
                    onBridgeEvent(PptistBridgeEvent.AIRequest(action, payload))
                }

                @JavascriptInterface
                fun onError(message: String) {
                    Timber.e("PPTist error: $message")
                    onBridgeEvent(PptistBridgeEvent.Error(message))
                }

                @JavascriptInterface
                fun onLog(message: String) {
                    Timber.d("PPTist log: $message")
                }
            },
            BRIDGE_NAME
        )

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Timber.d("PPTist page loaded: $url")
                // Inject a small bootstrap script that hooks into PPTist's
                // event emitter and forwards events to AndroidBridge.
                view?.evaluateJavascript(BOOTSTRAP_JS, null)
            }
        }
        webChromeClient = WebChromeClient()

        // Load the bundled PPTist from assets
        loadUrl("file:///android_asset/pptist/index.html")
    }
}

/**
 * Bridge events that PPTist can emit to Kotlin.
 */
sealed interface PptistBridgeEvent {
    /**
     * PPTist finished booting and is ready to receive commands.
     */
    data object Ready : PptistBridgeEvent

    /**
     * The current presentation state changed (slides, elements, etc).
     * jsonState is the full serialized PPTist state.
     */
    data class StateChanged(val jsonState: String) : PptistBridgeEvent

    /**
     * User navigated to a different slide.
     */
    data class SlideChanged(val slideIndex: Int) : PptistBridgeEvent

    /**
     * User selected (or deselected) an element.
     * objectId is null when nothing is selected.
     */
    data class SelectionChanged(val objectId: String?) : PptistBridgeEvent

    /**
     * User requested export. format is "pptx" | "json" | "png".
     * base64Data is the encoded file data.
     */
    data class ExportRequest(val format: String, val base64Data: String) : PptistBridgeEvent

    /**
     * AI action requested by user from within PPTist.
     * action: "generate" | "edit_object" | "edit_slide" | "improve_design"
     * payload: JSON with action-specific data
     */
    data class AIRequest(val action: String, val payload: String) : PptistBridgeEvent

    /**
     * PPTist reported an error.
     */
    data class Error(val message: String) : PptistBridgeEvent
}

/**
 * JavaScript that PPTist runs on startup to wire its internal events
 * to the AndroidBridge. PPTist uses mitt (event emitter) and Pinia store.
 *
 * This is intentionally tiny — just enough to forward user-visible events.
 * For more advanced control (e.g. programmatic load), we use evaluateJavascript
 * from Kotlin side.
 */
private const val BOOTSTRAP_JS = """
(function() {
  function safe(fn) {
    try { fn(); } catch (e) { console.error('bootstrap error:', e); }
  }
  // PPTist takes a moment to mount Vue; poll for the global `editor` or store
  var attempts = 0;
  var max = 50; // 5s at 100ms intervals
  function checkReady() {
    attempt++;
    // PPTist exposes its main store via window.__editor or via the global app
    // We detect "ready" by checking for a specific DOM element in the app
    var editorRoot = document.querySelector('#app') || document.querySelector('.editor') || document.querySelector('.presentation');
    if (editorRoot && editorRoot.children.length > 0) {
      safe(function() {
        if (window.AndroidBridge) window.AndroidBridge.onReady();
        if (window.AndroidBridge) window.AndroidBridge.onLog('PPTist ready');
      });
      return;
    }
    if (attempt < max) setTimeout(checkReady, 100);
    else safe(function() {
      if (window.AndroidBridge) window.AndroidBridge.onError('PPTist failed to start within 5s');
    });
  }
  // Give Vue a tick before checking
  setTimeout(checkReady, 200);
})();
"""

internal const val BRIDGE_NAME = "AndroidBridge"

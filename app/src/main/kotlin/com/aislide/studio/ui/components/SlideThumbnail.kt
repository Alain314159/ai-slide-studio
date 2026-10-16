package com.aislide.studio.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A mini-WebView that renders a slide's HTML at thumbnail size.
 *
 * Used in the editor's slide list to show a real preview of each slide instead
 * of just the slide title.
 *
 * Note: each thumbnail is a separate WebView. Compose's AndroidView caches
 * the WebView across recompositions of the same LazyColumn item, so we don't
 * create a new WebView on every recompose — only on first appearance.
 *
 * Performance: with 50+ slides this gets heavy. For large presentations, swap
 * to a single captured PNG per slide (saved at slide save time) — TODO v0.3.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SlideThumbnail(
    html: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier  // caller controls size, we just fill it
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = false  // thumbnails don't need JS
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            },
            update = { web ->
                web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

package com.aislide.studio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * AI Slide Studio Application.
 *
 * Architecture v2 (PPTist-fork based):
 * - WebView (PPTist Vue.js editor) → Kotlin native bridge
 * - Kotlin handles: LLM API, persistence, native UI chrome
 * - PPTist handles: visual slide editing (drag-drop, charts, tables, etc.)
 *
 * Workflow:
 * 1. User opens editor → Kotlin loads PPTist in WebView
 * 2. User edits slides visually in PPTist
 * 3. PPTist emits events → Kotlin bridge receives them
 * 4. User taps "Generate with AI" → Kotlin calls LLM → injects JSON into PPTist
 * 5. User taps "Export PPTX" → PPTist generates via pptxgenjs → Kotlin saves via SAF
 */
@HiltAndroidApp
class AiSlideApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

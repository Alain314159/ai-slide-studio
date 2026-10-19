package com.aislide.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aislide.studio.pptist.PptistEditorScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity — hosts the PPTist WebView editor.
 *
 * Architecture v2: the editor is web-based (PPTist) wrapped in a Compose
 * AndroidView. All other functionality (LLM, persistence, navigation chrome)
 * is native Kotlin.
 *
 * For now (single-activity MVP), this activity directly shows the PPTist
 * editor. As the app grows, navigation will be added for:
 * - Home screen (project list)
 * - Settings screen (API key)
 * - Editor screen (PPTist WebView) ← this is what we show now
 * - Presentation screen
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AiSlideStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PptistEditorScreen()
                }
            }
        }
    }
}

@Composable
private fun AiSlideStudioTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF5B6CFF),
        background = Color(0xFF0F1117),
        surface = Color(0xFF1A1D26)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

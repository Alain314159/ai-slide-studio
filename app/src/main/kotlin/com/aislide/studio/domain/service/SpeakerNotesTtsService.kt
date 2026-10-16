package com.aislide.studio.domain.service

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android's [TextToSpeech] for reading slide speaker notes aloud.
 *
 * Use case: the user wants to "hear" their presentation before delivering it,
 * to check timing and pronunciation.
 *
 * Lifecycle: TTS engine is initialized lazily on first [speak] call. We
 * hold a single instance and reuse it. Calling [stop] cancels any in-flight
 * speech.
 *
 * Locale: defaults to Spanish (es-ES) since the app is Spanish-first.
 * Could be made configurable in Settings later.
 *
 * Rule of android skill: TextToSpeech requires Context — we inject
 * ApplicationContext so it's tied to the app lifecycle, not a specific Activity.
 */
@Singleton
class SpeakerNotesTtsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private fun ensureInit() {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("es", "ES"))
                    _ready.value = result != TextToSpeech.LANG_MISSING_DATA &&
                                   result != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _speaking.value = true }
                override fun onDone(utteranceId: String?) { _speaking.value = false }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { _speaking.value = false }
            })
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        ensureInit()
        if (_ready.value) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "slide_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        tts?.stop()
        _speaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _ready.value = false
        _speaking.value = false
    }
}

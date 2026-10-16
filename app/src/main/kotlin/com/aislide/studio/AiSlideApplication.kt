package com.aislide.studio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for AI Slide Studio.
 *
 * Personal-use, offline-first Android app for creating presentations with AI.
 * Uses Hilt for DI (Rule of android skill: di_framework=hilt).
 *
 * SECURITY NOTE (Rule 7 of android skill): Nothing in the APK is a secret.
 * The LLM API key is NOT shipped in BuildConfig or strings.xml — it's provided
 * by the user in Settings and stored in DataStore (encrypted user-preferences).
 */
@HiltAndroidApp
class AiSlideApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupTimber()
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Production crash reporting: integrate Play Vitals in a future phase.
    }
}

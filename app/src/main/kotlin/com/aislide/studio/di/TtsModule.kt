package com.aislide.studio.di

import com.aislide.studio.domain.service.SpeakerNotesTtsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TtsModule {

    @Provides @Singleton
    fun provideSpeakerNotesTtsService(
        @ApplicationContext context: android.content.Context
    ): SpeakerNotesTtsService = SpeakerNotesTtsService(context)
}

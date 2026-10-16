package com.aislide.studio.di

import com.aislide.studio.data.repository.LlmRepository
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.PptxExportEngine
import com.aislide.studio.domain.usecase.AddSlideObjectUseCase
import com.aislide.studio.domain.usecase.EditObjectWithAiUseCase
import com.aislide.studio.domain.usecase.ExportToPptxUseCase
import com.aislide.studio.domain.usecase.ExportToPptxVectorUseCase
import com.aislide.studio.domain.usecase.GenerateImageUseCase
import com.aislide.studio.domain.usecase.GeneratePresentationUseCase
import com.aislide.studio.domain.usecase.ImportPptxUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides @Singleton
    fun provideHtmlSlideRenderer(): HtmlSlideRenderer = HtmlSlideRenderer()

    @Provides @Singleton
    fun providePptxZipWriter(): com.aislide.studio.domain.service.PptxZipWriter =
        com.aislide.studio.domain.service.PptxZipWriter()

    @Provides @Singleton
    fun providePptxExportEngine(
        zipWriter: com.aislide.studio.domain.service.PptxZipWriter
    ): PptxExportEngine = PptxExportEngine(zipWriter)

    @Provides @Singleton
    fun provideGeneratePresentationUseCase(
        llm: LlmRepository, repo: ProjectRepository, renderer: HtmlSlideRenderer
    ) = GeneratePresentationUseCase(llm, repo, renderer)

    @Provides @Singleton
    fun provideEditObjectWithAiUseCase(
        llm: LlmRepository, repo: ProjectRepository, renderer: HtmlSlideRenderer,
        history: com.aislide.studio.domain.service.SlideHistoryManager
    ) = EditObjectWithAiUseCase(llm, repo, renderer, history)

    @Provides @Singleton
    fun provideAddSlideObjectUseCase(
        repo: ProjectRepository, renderer: HtmlSlideRenderer, history: com.aislide.studio.domain.service.SlideHistoryManager
    ) = AddSlideObjectUseCase(repo, renderer, history)

    @Provides @Singleton
    fun provideExportToPptxUseCase(
        repo: ProjectRepository, engine: PptxExportEngine,
        @dagger.hilt.android.qualifiers.ApplicationContext ctx: android.content.Context
    ) = ExportToPptxUseCase(repo, engine, ctx)

    @Provides @Singleton
    fun provideExportToPptxVectorUseCase(
        repo: ProjectRepository, renderer: HtmlSlideRenderer,
        zipWriter: com.aislide.studio.domain.service.PptxZipWriter,
        @dagger.hilt.android.qualifiers.ApplicationContext ctx: android.content.Context
    ) = ExportToPptxVectorUseCase(repo, renderer, zipWriter, ctx)

    @Provides @Singleton
    fun provideGenerateImageUseCase(
        llm: LlmRepository,
        imageStorage: com.aislide.studio.data.local.ImageStorageManager
    ) = GenerateImageUseCase(llm, imageStorage)

    @Provides @Singleton
    fun provideImportPptxUseCase(
        repo: ProjectRepository, renderer: HtmlSlideRenderer,
        imageStorage: com.aislide.studio.data.local.ImageStorageManager,
        @dagger.hilt.android.qualifiers.ApplicationContext ctx: android.content.Context
    ) = ImportPptxUseCase(repo, renderer, imageStorage, ctx)

    @Provides @Singleton
    fun provideImageStorageManager(
        @dagger.hilt.android.qualifiers.ApplicationContext ctx: android.content.Context
    ): com.aislide.studio.data.local.ImageStorageManager =
        com.aislide.studio.data.local.ImageStorageManager(ctx)

    @Provides @Singleton
    fun provideValidateApiKeyUseCase(
        llm: LlmRepository,
        settings: com.aislide.studio.data.local.SettingsDataStore
    ) = com.aislide.studio.domain.usecase.ValidateApiKeyUseCase(llm, settings)

    @Provides @Singleton
    fun provideSlideHistoryManager() = com.aislide.studio.domain.service.SlideHistoryManager()

    @Provides @Singleton
    fun provideUndoRedoUseCase(
        repo: ProjectRepository,
        renderer: HtmlSlideRenderer,
        history: com.aislide.studio.domain.service.SlideHistoryManager
    ) = com.aislide.studio.domain.usecase.UndoRedoUseCase(repo, renderer, history)

    @Provides @Singleton
    fun provideUpdateObjectStyleUseCase(
        repo: ProjectRepository,
        renderer: HtmlSlideRenderer,
        history: com.aislide.studio.domain.service.SlideHistoryManager
    ) = com.aislide.studio.domain.usecase.UpdateObjectStyleUseCase(repo, renderer, history)

    @Provides @Singleton
    fun provideUpdateObjectTextUseCase(
        repo: ProjectRepository,
        renderer: HtmlSlideRenderer,
        history: com.aislide.studio.domain.service.SlideHistoryManager
    ) = com.aislide.studio.domain.usecase.UpdateObjectTextUseCase(repo, renderer, history)
}

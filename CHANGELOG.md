# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] — 2026-10-17

### ✨ v1.0 release — first working version

This release marks the first version where the app **compiles cleanly in CI**
and **all 96 unit tests pass**. The APK is downloadable directly from the
GitHub Actions artifacts.

### Added

- **AI generation** from free-text prompts using GLM (z.ai) or any OpenAI-compatible API
- **Per-object AI editing**: tap an object in the slide, describe what you want,
  the LLM modifies ONLY that object — not the whole slide
- **Tap-to-select** in WebView via JavaScript interface (ObjectSelectionBridge)
- **Manual editor**: text, bullets, images, font size/weight/color, alignment,
  background color, border radius, shadow, HEX color input
- **Import .pptx** via manual ZIP+regex parsing (text and image extraction)
- **Export to .pptx** in two modes:
  - EDITABLE: text remains selectable in PowerPoint
  - VISUAL: high-resolution screenshot per slide, looks pixel-perfect
- **Share .pptx** via Android share intent
- **Presentation mode**: full-screen WebView with swipe navigation
- **Speaker notes**: visible in presentation mode, read aloud via TTS
- **Undo/redo** per slide (50-entry stack, isolated per slide ID)
- **Project search** in Home screen
- **API key validation** with "Probar conexión" button in Settings
- **Auto-save** with "Guardado ✓" indicator
- **Persistent image storage** (filesDir instead of cacheDir)

### Technical

- **MVVM + Clean Architecture + Hilt DI**
- **Jetpack Compose** + Material 3
- **Room** for projects and slides persistence
- **DataStore** for settings (API key, provider, endpoint, model)
- **Ktor 3.0** for HTTP client (OpenAI-compatible)
- **Manual PPTX generation** (no Apache POI — Android doesn't support AWT)
- **96 unit tests** covering all use cases, repositories, and services

### CI/CD

- GitHub Action workflow that builds the APK and runs all tests on every push
- APK artifact available for download directly from Actions page
- Build-reports-on-failure artifact for debugging

## [0.3.0] — 2026-10-17

### Added

- ImageStorageManager for persistent image storage
- ValidateApiKeyUseCase for testing API key validity
- Search in Home screen
- "Guardado ✓" indicator in editor
- Share intent for .pptx export

## [0.2.0] — 2026-10-17

### Added

- Style editor bottom sheet (sliders, chips, color palette)
- Speaker notes with TTS playback
- Reorder slides (move up/down buttons)
- Undo/redo with per-slide HistoryManager
- Slide thumbnails (mini WebView) in editor list

## [0.1.0] — 2026-10-17

### Added

- Initial project scaffold (Kotlin 2.0.21 + Compose + Hilt)
- Data layer: Room (Project, Slide, SlideObject), DataStore for settings
- Remote layer: LlmApiService (Ktor), DTOs, LlmProvider enum
- Domain services: HtmlSlideRenderer (interactive JS bridge), PptxExportEngine
- Domain use cases: Generate, EditObjectWithAi, Import, Export
- UI screens: Home, AiGenerate, Editor, SlideEditor, Settings
- Material 3 dark theme
- Navigation graph for 7 routes

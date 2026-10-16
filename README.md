# AI Slide Studio

> Personal Android app for creating PowerPoint presentations with AI assistance. Built in October 2026.

## 📱 What it does

- Generate complete presentations from a free-text prompt via GLM (z.ai)
- Edit individual slide objects (titles, bullets, images) with AI assistance
- Edit manually with all the standard PowerPoint features
- Import existing `.pptx` files
- Export to `.pptx` with **editable text** (Apache POI + EMU mapping)

## 🏗️ Architecture

| Layer | Stack |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Persistence | Room (SQLite) + DataStore (settings) |
| Async | Coroutines + Flow |
| Networking | Ktor (OpenAI-compatible API) |
| LLM | GLM (z.ai) by default, BYOK pattern |
| Export | Apache POI 5.x with EMU mapping |
| HTML render | Android WebView |

## 📦 Project structure

```
ai-slide-studio/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/aislide/studio/
│       │   ├── AiSlideApplication.kt
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── dao/ (ProjectDao, SlideDao)
│       │   │   │   ├── database/ (AppDatabase, AppConverters, AppJson)
│       │   │   │   ├── entity/ (ProjectEntity, SlideEntity)
│       │   │   │   └── SettingsDataStore.kt
│       │   │   ├── model/ (Presentation, Slide, SlideObject, Palette, Typography)
│       │   │   ├── remote/
│       │   │   │   ├── dto/ (ChatDto, ImageDto, PresentationDto)
│       │   │   │   ├── LlmApiService.kt
│       │   │   │   └── LlmProvider.kt
│       │   │   └── repository/ (ProjectRepository, LlmRepository)
│       │   ├── di/ (DatabaseModule, NetworkModule, DomainModule)
│       │   ├── domain/
│       │   │   ├── service/ (HtmlSlideRenderer, PptxExportEngine)
│       │   │   └── usecase/ (Generate, Edit, Import, Export)
│       │   └── ui/
│       │       ├── navigation/AppNavigation.kt
│       │       ├── theme/ (Color, Type, Theme)
│       │       └── screens/ (home, ai, editor, slide, settings)
│       └── res/ (values, drawable, xml, mipmap)
├── gradle/
│   ├── libs.versions.toml  (version catalog)
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── .gitignore
```

## 🚀 Setup (for Alain)

1. **Clone this repo to your machine** (once we push it to GitHub)
2. **Open in Android Studio** (Hedgehog or newer — Kotlin 2.0.21 support)
3. **Wait for Gradle sync** — it will download all dependencies
4. **Open Settings screen on the app** and paste your GLM API key
5. **Run the app** on a device/emulator (minSdk 26 = Android 8.0+)

## 🔑 API key configuration

The app uses a **BYOK (Bring Your Own Key)** pattern:

1. Open the app → Settings (gear icon top-right)
2. Select provider (default: GLM/Z.ai)
3. Paste your API key
4. Endpoint and model auto-fill from `LlmProvider`

The key is stored in DataStore (Android preferences). For v0.1 personal use, this
is acceptable. For production, swap to `EncryptedSharedPreferences` with Tink.

## 🎯 Status (v0.1)

| Feature | Status |
|---|---|
| Project scaffold + Gradle + manifest | ✅ Done |
| Data layer (Room, DAOs, entities) | ✅ Done |
| LLM API service (Ktor, OpenAI-compatible) | ✅ Done |
| Repositories (Project, LLM) | ✅ Done |
| Use cases (Generate, Edit, Import, Export) | ✅ Done (skeletons) |
| HTML slide renderer | ✅ Done |
| PPTX export engine (Apache POI + EMU) | ✅ Done (skeleton) |
| UI screens (Home, AI, Editor, Slide, Settings) | ✅ Done (placeholders) |
| PPTX SVG embed export | ⏳ Phase 2 |
| Image generation integration | ⏳ Phase 2 |
| Object tap selection | ⏳ Phase 2 |
| Undo/redo | ⏳ Phase 2 |
| PDF export | ⏳ Deferred |

## 🛠️ Next steps

After cloning to Android Studio:
1. Run Gradle sync — verify no missing dependencies
2. Run on emulator — verify the app launches
3. Open Settings — verify provider dropdown shows GLM
4. Paste API key, navigate to "Crear con IA", write a prompt
5. Verify a presentation is generated and saved

## 📝 License

Personal-use project. All rights reserved.

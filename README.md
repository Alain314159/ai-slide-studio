# AI Slide Studio

> Personal Android app for creating PowerPoint presentations with AI assistance.
> v1.0 — compilación limpia, 96 tests pasan en CI. Octubre 2026.

## 🎉 Estado v1.0

- ✅ **Compila en CI** (GitHub Actions) con JDK 17 + AGP 8.7.2 + Kotlin 2.0.21
- ✅ **96 tests unitarios** pasan en cada push
- ✅ **APK artifact** disponible para descarga directa desde GitHub Actions
- ✅ Repo: https://github.com/Alain314159/ai-slide-studio

## 📱 Qué hace

- **Genera presentaciones** desde un prompt de texto en lenguaje natural usando GLM (z.ai) o cualquier API compatible con OpenAI
- **Edita objetos individuales con IA**: tocas un objeto (título, bullet, imagen) y le describes a la IA cómo lo quieres. La IA solo cambia ese objeto.
- **Editor manual completo**: añade texto, bullets, imágenes; cambia fuentes, colores, alineación, bordes
- **Tap-to-select nativo**: toca un objeto en el WebView y se resalta, listo para editar con IA o manualmente
- **Importa `.pptx`** existentes (extracción de texto e imágenes)
- **Exporta `.pptx` editable** (texto seleccionable en PowerPoint) usando ZIP+XML manuales
- **Exporta `.pptx` visual** (imagen PNG por slide, calidad perfecta)
- **Comparte el `.pptx`** exportado por email, Drive, etc.
- **Modo presentación full-screen** con swipe horizontal
- **Notas del orador**: visibles en modo presentación, leídas en voz alta con TTS
- **Undo/redo** por slide (historial aislado por slide)
- **Búsqueda de proyectos** en la home
- **Auto-save**: cada cambio se persiste inmediatamente, con indicador "Guardado ✓"
- **Imágenes persistentes**: las imágenes IA no se pierden al limpiar el cache

## 📦 Cómo probarla sin Android Studio

### Opción 1: Descarga el APK del último CI

1. Ve a https://github.com/Alain314159/ai-slide-studio/actions
2. Abre el último workflow run (debería estar verde ✅)
3. En la sección "Artifacts" al final, descarga `app-debug-apk`
4. Descomprime el .zip → tienes `app-debug.apk`
5. En tu Android (8.0+): instala el APK (permitir "orígenes desconocidos")
6. Abre la app → Settings → pega tu API key de GLM
7. Vuelve atrás, toca "Nueva con IA", describe tu presentación

### Opción 2: Si tienes Android Studio

```bash
git clone https://github.com/Alain314159/ai-slide-studio
cd ai-slide-studio
# Abrir en Android Studio, esperar Gradle sync
# Run on device/emulator
```

## 🏗️ Stack técnico

| Capa | Tech |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Clean Architecture |
| DI | Hilt 2.52 |
| Persistencia | Room (SQLite) + DataStore |
| Async | Coroutines + Flow |
| Networking | Ktor 3.0 (OpenAI-compatible) |
| LLM | GLM (z.ai) por defecto, BYOK pattern |
| HTML render | Android WebView (interactive con JS bridge) |
| PPTX export | Manual ZIP+XML (sin Apache POI — Android no soporta AWT) |
| PPTX import | Manual ZIP+regex parsing |
| TTS | Android TextToSpeech nativo |
| Imágenes | ImageStorageManager (filesDir persistente) |

## 🧠 Decisiones técnicas

### ¿Por qué no Apache POI?

Apache POI para PPTX requiere `java.awt.*` (Color, Rectangle, Dimension), pero Android no incluye AWT. La compilación falla con 30+ unresolved references.

**Solución**: implementar PPTX generation manualmente con `java.util.zip` + XML templates. El resultado es un .pptx ZIP perfectamente válido, sin dependencias externas.

### ¿Por qué HTML/CSS para slides?

La IA (GLM, GPT, Claude) conoce HTML/CSS mucho mejor que XML de PowerPoint. Renderizar slides como HTML en WebView da:
- Libertad visual total (gradientes, sombras, tipografías web)
- Tap-to-select nativo via JS interface
- Mismo HTML para vista previa y exportación

### ¿Por qué BYOK para la API?

Patrón "Bring Your Own Key": el usuario pega su API key en Settings. No se incluye en el APK. Compatible con cualquier LLM compatible con OpenAI (GLM, OpenAI, Anthropic, custom).

## 📁 Estructura del proyecto

```
ai-slide-studio/
├── .github/workflows/android.yml  # CI: build + tests en cada push
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/aislide/studio/
│       │   │   ├── AiSlideApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/  (Room, DAOs, ImageStorageManager, SettingsDataStore)
│       │   │   │   ├── model/  (Presentation, Slide, SlideObject, Palette, Typography)
│       │   │   │   ├── remote/ (LlmApiService, LlmProvider, DTOs)
│       │   │   │   └── repository/  (ProjectRepository, LlmRepository)
│       │   │   ├── di/  (DatabaseModule, NetworkModule, DomainModule, TtsModule)
│       │   │   ├── domain/
│       │   │   │   ├── service/  (HtmlSlideRenderer, PptxZipWriter, PptxExportEngine, HistoryManager, SpeakerNotesTtsService)
│       │   │   │   └── usecase/  (13 use cases)
│       │   │   └── ui/
│       │   │       ├── components/  (StyleEditorSheet, SlideThumbnail, ReorderableList)
│       │   │       ├── navigation/
│       │   │       ├── screens/  (home, ai, editor, slide, settings, presentation)
│       │   │       └── theme/
│       │   └── res/  (values, drawable, mipmap-anydpi-v26, xml)
│       └── test/kotlin/com/aislide/studio/  (96 tests unitarios)
├── gradle/libs.versions.toml  (version catalog)
└── README.md (este archivo)
```

## ✅ Tests unitarios (96 tests)

| Test Suite | # Tests | Qué cubre |
|---|---|---|
| HistoryManagerTest | 11 | Undo/redo, capacity, FIFO eviction |
| ChatDtoSerializationTest | 7 | OpenAI wire format, snake_case |
| PresentationDtoDeserializationTest | 7 | LLM response decoding, tolerancia a nulls |
| HtmlSlideRendererTest | 16 | HTML output, escape, modo interactivo JS |
| LlmProviderTest | 7 | Enum, fallback para IDs desconocidos |
| ProjectRepositoryTest | 7 | Mapeo entidad↔dominio |
| EditObjectWithAiUseCaseTest | 7 | Edición por objeto con IA |
| GeneratePresentationUseCaseTest | 6 | Mapeo DTO→dominio |
| UpdateObjectStyleUseCaseTest | 3 | Estilo manual |
| UpdateObjectTextUseCaseTest | 4 | Texto/bullets manual |
| UndoRedoUseCaseTest | 5 | Snapshot/undo/redo |
| ValidateApiKeyUseCaseTest | 7 | 4 ramas (Success/InvalidKey/WrongEndpoint/Error) |
| ProjectRepositoryMoveSlideTest | 7 | Reorder de slides |
| **Total** | **96** | |

## 📋 Licencia

Personal-use project. All rights reserved.

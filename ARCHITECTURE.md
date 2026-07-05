# Xi Architecture

## Overview

Xi is an AI-powered English learning Android app with two core features:
1. **Translate** — AI translation between 20 languages via floating overlay or in-app
2. **Essay Correction** — AI-powered essay grading with grammar/vocabulary/structure/style analysis

## Tech Stack

- **Language**: Kotlin 100%
- **UI**: Jetpack Compose + Material3
- **DI**: Dagger Hilt
- **Network**: Retrofit + OkHttp + Gson
- **State**: DataStore Preferences, StateFlow
- **Min SDK**: 26 / Target: 36

## Architecture: Simplified MVVM

```
┌─────────────────────────────────────────────┐
│  Compose UI (Screen/Component)              │
│  collects uiState: StateFlow<UiState>       │
└──────────────────┬──────────────────────────┘
                   │ user actions
┌──────────────────▼──────────────────────────┐
│  ViewModel                                  │
│  - translates events → repository calls     │
│  - manages UiState                          │
│  - NO Context / NO Bitmap                   │
└──────────────────┬──────────────────────────┘
                   │  suspend fun callWithRetry
┌──────────────────▼──────────────────────────┐
│  ApiRepository (abstract base)              │
│  - getApi() with proxy support              │
│  - callWithRetry(maxRetries=2, backoff)     │
│  - network check + API key validation       │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Repository (Translation / Essay)           │
│  - builds prompts                           │
│  - parses responses                         │
└──────────────────┬──────────────────────────┘
                   │ Retrofit
┌──────────────────▼──────────────────────────┐
│  ApiProvider + OpenAiApi (Retrofit)         │
│  - creates OkHttpClient (with proxy)        │
└─────────────────────────────────────────────┘
```

## Key Design Decisions

### ApiRepository Base Class
Eliminates getApi() + retry loop duplication across repositories.
All repositories extend it and use `callWithRetry { }`.

### MediaProcessor
Singleton service handling Bitmap/PDF operations off the main thread.
ViewModels delegate to MediaProcessor and store only URIs, not Bitmaps.

### ThinkingSelector
Reusable composable extracted to avoid duplication across translate/essay screens.
Collapsible FilterChip grid with animated visibility.

### Proxy Support
Proxy configuration flows: SettingsScreen → DataStore → ApiRepository → ApiProvider → OkHttpClient.proxy().
Previously stored but never applied — now fully wired.

## Data Flow

### Translate
```
User Input → TranslateScreen → TranslateViewModel.translate()
  → TranslationRepository.translate()
    → ApiRepository.callWithRetry → getApi() → Retrofit POST /chat/completions
  → Result<String> → UiState.resultText
```

### Essay Correction
```
Essay Text/Image/PDF → EssayScreen → EssayViewModel.correctEssay()
  → EssayRepository.correctEssay() / correctEssayFromImage()
    → ApiRepository.callWithRetry → getApi() → Retrofit POST
  → Result<CorrectionResult> → UiState (corrections + essay + score + tips)
```

### Image Input
```
Camera/Gallery/PDF → Uri → EssayViewModel.setImageUri/setPdfUri
  → MediaProcessor.loadBitmapFromUri/renderPdfPages (Dispatchers.IO)
  → UiState.imageUri (NOT bitmap)
→ on correct: EssayRepository.uriToBase64 (Dispatchers.IO, recycles bitmaps)
```

## Test Strategy

| Layer | Tool | Coverage |
|-------|------|----------|
| API Models | JUnit | ChatRequest/Response serialization |
| UiState | JUnit | State transitions (EssayViewModelTest) |
| Repository | JUnit + mockk | Translation/Essay API calls |

## Remaining Work

- OverlayService > 500 lines — should be split into Service + composable files
- UI design: replace Material Icons with custom SVG, adjust corner radius to 28dp

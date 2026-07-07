# LingoFlow — AI English Learning Assistant

## Product Brief

Android English learning app with two core features:

1. **Quick Translation** — system-level floating window overlay for instant translation from any app
2. **Essay Correction** — AI-powered English essay correction with diff view and corrected version

Plus a **Settings** screen for API subscription management.

## Target Users

Chinese English learners who need quick translation while reading/using other apps, and students who want AI essay correction.

## Tech Stack

| Component              | Version          |
| ---------------------- | ---------------- |
| AGP                    | 9.0.1            |
| Kotlin                 | 2.3.20           |
| Compose BOM            | 2026.06.00       |
| Hilt                   | 2.59.2           |
| KSP                    | 2.3.9            |
| DataStore              | 1.2.1            |
| Retrofit + OkHttp      | latest           |
| Navigation3            | 1.1.3            |
| minSdk                 | 26 (Android 8.0) |
| targetSdk / compileSdk | 36               |

## Architecture

```
MVVM + Clean Architecture

┌─────────────────────────────────────┐
│  UI Layer (Compose)                 │
│  ├── TranslateScreen                │
│  ├── EssayCorrectionScreen          │
│  └── SettingsScreen                 │
├─────────────────────────────────────┤
│  ViewModel Layer                    │
│  ├── TranslateViewModel             │
│  ├── EssayViewModel                 │
│  └── SettingsViewModel              │
├─────────────────────────────────────┤
│  Domain / Repository                │
│  ├── TranslationRepository          │
│  ├── EssayRepository                │
│  └── SettingsRepository             │
├─────────────────────────────────────┤
│  Data Layer                         │
│  ├── OpenAI-compatible API (Retrofit)│
│  ├── DataStore (settings/prefs)     │
│  └── Room (essay history, optional) │
├─────────────────────────────────────┤
│  Service Layer                      │
│  ├── OverlayService (floating window)│
│  └── ClipboardService (auto-detect) │
└─────────────────────────────────────┘
```

## Screen Inventory

### Screen 1: Translation (翻译)

- Text input field (multiline)
- Source/target language selector (EN↔ZH default)
- "Translate" button → calls LLM API
- Result area with copy button
- History of recent translations (scrollable)
- **FAB** to enable floating overlay service
- Loading/error/empty states

### Screen 2: Essay Correction (作文批改)

- Large text input area for English essay
- "Submit for Correction" button
- Results displayed in two tabs:
  - **Correction View**: inline diff with color-coded corrections (red=deleted, green=added, yellow=grammar note)
  - **Corrected Version**: clean corrected essay for reference
- Copy/export corrected version
- Loading/error/empty states

### Screen 3: Settings (设置)

- API Base URL input (default: OpenCode Go endpoint)
- API Key input (masked)
- Model selector (dropdown, default: mimo-v2.5)
- "Test Connection" button with status indicator
- Proxy settings (optional)
- About/version info

### Overlay (Floating Window System)

- **Floating Bubble**: small draggable icon (translucent, always on top)
- **Expanded Panel**: tap bubble → expand to translation mini-panel
  - Input field (paste or type)
  - Quick translate button
  - Result display
  - Copy result / close
- **Clipboard Monitor**: optional — detect copied text, auto-show translation
- Requires `SYSTEM_ALERT_WINDOW` permission
- Foreground Service with persistent notification

## Navigation

Bottom Navigation Bar:

1. Translate (翻译)
2. Essay (作文)
3. Settings (设置)

Overlay is independent of navigation — launched via system permission + service.

## Data Model

### Settings (DataStore)

```
api_base_url: String = "https://api.opencode.ai/v1"
api_key: String = ""
model: String = "mimo-v2.5"
proxy_enabled: Boolean = false
proxy_host: String = ""
proxy_port: Int = 0
```

### Translation History (Room, optional Phase 2)

```
TranslationEntity:
  id: Long (auto)
  source_text: String
  translated_text: String
  source_lang: String
  target_lang: String
  timestamp: Long
```

## API Integration

OpenAI-compatible chat completions endpoint:

```
POST {base_url}/chat/completions
Authorization: Bearer {api_key}
{
  "model": "{model}",
  "messages": [
    {"role": "system", "content": "You are a professional translator..."},
    {"role": "user", "content": "Translate: {text}"}
  ]
}
```

Essay correction prompt:

```
system: "You are an English teacher. Correct the essay, explain each correction, 
and provide a corrected version."
user: "Please correct this essay:\n\n{text}"
```

Response parsing: extract `choices[0].message.content`

## Implementation Slices

### Slice 1: Project Skeleton + Navigation

- Create project with AGP 9.0 + Compose + Hilt
- Bottom navigation with 3 tabs
- Empty screen stubs
- Build + run verification

### Slice 2: Settings Screen + API Layer

- DataStore for settings persistence
- Retrofit client with configurable base URL, API key, model
- Connection test endpoint
- Settings UI with all fields
- Build + unit test

### Slice 3: Translation Screen

- Translation UI (input, language selector, result)
- TranslationViewModel calling API
- Loading/error/empty states
- Copy result functionality

### Slice 4: Essay Correction Screen

- Essay input UI
- EssayViewModel calling API with correction prompt
- Diff view (color-coded corrections)
- Corrected version tab
- Copy/export

### Slice 5: Floating Overlay

- SYSTEM_ALERT_WINDOW permission request flow
- ForegroundService with notification
- Floating bubble (draggable ComposeView in WindowManager)
- Expanded translation panel
- Clipboard monitoring (optional toggle)

### Slice 6: Polish + QA

- Visual review with screenshots
- Edge cases (empty input, API error, no permission)
- Performance check
- Final APK build

## Design Tokens

Following user's UI preference:

- Background: warm ivory/米黄 `#FAF6F0`
- Text: dark brown-gray `#3D3530`
- Accent: low-saturation Monet tones
- Corners: 24-32dp large radius
- Typography: clean, high contrast
- Minimal dividers, no heavy shadows
- Floating overlay: semi-transparent, soft shadow

## Quality Strategy

- Unit tests: ViewModel logic, API response parsing, settings persistence
- Instrumented test: navigation flow, overlay permission flow
- Visual evidence: screenshots for every screen/state
- Lint: clean build required
- Manual QA matrix in emulator

## Non-Goals (v1)

- Speech/TTS integration
- Offline translation
- User accounts / cloud sync
- Gamification / flashcards
- Multiple language pairs (EN↔ZH only for v1)

## Decision Register

| Decision                                      | Rationale                                                                                     |
| --------------------------------------------- | --------------------------------------------------------------------------------------------- |
| Retrofit over Ktor                            | More mature Android ecosystem, simpler setup                                                  |
| Room for history                              | Structured queries, type safety                                                               |
| Hilt over manual DI                           | Standard Android DI, less boilerplate                                                         |
| SYSTEM_ALERT_WINDOW over AccessibilityService | Overlay approach is more flexible for custom UI, accessibility is heavier and more restricted |
| DataStore over SharedPreferences              | Modern, coroutine-native, type-safe                                                           |

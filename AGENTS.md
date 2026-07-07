# AGENTS.md — Xi

## Project Overview
AI-driven English learning Android app with floating translation overlay and essay correction.

## Build
```bash
source ~/.config/android-dev/env.sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

## Package
`luzzr.xi`

## Key Architecture
- MVVM + Hilt DI
- Jetpack Compose UI
- Retrofit for OpenAI-compatible API
- DataStore for settings
- SYSTEM_ALERT_WINDOW for overlay

## Screens
1. TranslateScreen — AI translation (20 languages)
2. EssayCorrectionScreen — essay correction with diff view
3. SettingsScreen — API config, model selection, connection test

## Overlay
- OverlayService with ComposeView in WindowManager
- Draggable bubble -> expanded translation panel
- Supports language selection and swap

## Testing
- Unit: ViewModels, API parsing, repository
- Visual: emulator screenshots per screen/state

## Version History
- v1.0.0: First official release — AI translation (20 languages, 3 thinking levels), essay correction (4 inputs, 4-dimension scoring), edge pill overlay

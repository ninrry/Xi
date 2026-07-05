# QA-MATRIX.md — LingoFlow

## Build Verification

| Check | Status | Evidence |
|-------|--------|----------|
| `./gradlew assembleDebug` | PASS | BUILD SUCCESSFUL |
| `./gradlew testDebugUnitTest` | PASS | All tests pass |
| APK size | 20MB | dist/lingoflow-v1.0.2-debug.apk |

## Screen Verification

| Screen | Element | Status | Notes |
|--------|---------|--------|-------|
| Translate | Title "翻译" | PASS | |
| Translate | Language chips EN/ZH | PASS | |
| Translate | Input field | PASS | |
| Translate | Translate button | PASS | |
| Translate | Overlay toggle | PASS | |
| Essay | Title "作文批改" | PASS | |
| Essay | Input field | PASS | |
| Essay | Correct button | PASS | |
| Settings | API URL field | PASS | Default correct |
| Settings | API Key field | PASS | |
| Settings | Model selector | PASS | |
| Settings | Test connection | PASS | |
| Settings | About section | PASS | |

## Overlay Verification

| Check | Status | Notes |
|-------|--------|-------|
| Service starts | PASS | dumpsys confirms running |
| Window created | PASS | 168x168px overlay window |
| No crash | PASS | 0 FATAL EXCEPTION |
| Permission flow | PASS | Opens system settings |

## Crash/ANR

| Metric | Count |
|--------|-------|
| FATAL EXCEPTION | 0 |
| ANR | 0 |
| Java exceptions | 0 |

## Performance

| Metric | Value |
|--------|-------|
| Cold start | ~2s (emulator) |
| Memory (overlay) | ~170MB peak |

# DECISIONS.md — LingoFlow

## D1: API Provider — OpenCode Go

- **Date:** 2026-07-04
- **Decision:** Use OpenCode Go as the default AI API provider
- **Endpoint:** `https://opencode.ai/zen/go/v1`
- **Default model:** `mimo-v2.5` (cheapest, ~30K requests/5h)
- **Rationale:** Low cost ($5 first month), OpenAI-compatible API, good model selection
- **Trade-off:** Requires internet, no offline mode

## D2: Overlay Implementation — SYSTEM_ALERT_WINDOW

- **Date:** 2026-07-04
- **Decision:** Use SYSTEM_ALERT_WINDOW overlay instead of AccessibilityService
- **Rationale:** Play Store compliant, more flexible for custom UI, lighter weight
- **Trade-off:** Requires manual permission grant, can't read other app's text automatically

## D3: Compose in Service

- **Date:** 2026-07-04
- **Decision:** Use ComposeView in OverlayService with manual LifecycleOwner setup
- **Rationale:** Consistent UI code between Activity and Service, Material3 components
- **Pitfall:** `view.context as Activity` crashes in Service context — use `as? Activity`
- **Pitfall:** `savedStateRegistryController.performRestore(null)` must be called before `super.onCreate()`

## D4: Navigation — Compose Navigation with Animations

- **Date:** 2026-07-04
- **Decision:** Use Navigation Compose 2.9.1 with slide/fade transitions
- **Rationale:** Standard Android navigation, type-safe, animation support
- **Not Navigation3:** API is still maturing, traditional NavHost more stable

## D5: DI — Hilt with Constructor Injection

- **Date:** 2026-07-04
- **Decision:** Hilt for DI with @Inject constructor on repositories and ViewModels
- **Rationale:** Standard Android DI, KSP support, minimal boilerplate
- **Note:** Hilt plugin resolved via buildscript classpath (not plugins DSL) due to Google Maven CDN issues in CN

## D6: Proxy Configuration

- **Date:** 2026-07-04
- **Decision:** HTTP proxy at 10808, configured in gradle.properties for JVM
- **Rationale:** dl.google.com blocked in CN, proxy enables Gradle dependency resolution
- **Note:** Emulator DNS may not resolve external hosts — real device needed for API testing

# Conventions

Terse imperative rules for writing and verifying code in Perspective - Live.

## Verification Commands

- Build debug APK: `./gradlew assembleDebug`
- Run unit tests: `./gradlew test`
- Run Kotlin linter: `./gradlew detekt`
- Run Markdown linter: `npx markdownlint-cli2 "**/*.md"`

## Code Style & Formatting

- Write in Kotlin following standard Kotlin conventions.
- Format code with 4 spaces for indentation.
- Keep line lengths under 120 characters where possible.
- Avoid wildcard imports except for `java.util.*` where configured.
- Avoid magic numbers by extracting named constants.

## Architecture & Layering

- Maintain strict layer boundaries: UI in `settings`, calculation logic in `modules`, drawing in `rendering`, persistence and data models in `data`, wallpaper runtime in `service`, general helpers in `utils`.
- Use MVVM with unidirectional data flow (`SettingsViewModel`, `StateFlow` collected via `collectAsStateWithLifecycle()`).
- Keep calculation modules free of Android UI/View dependencies.
- Use dependency injection through ViewModel factories (`SettingsViewModelFactory`).

## Compatibility & Migrations

- Target Android 17 (`minSdk = 37`, `targetSdk = 37`, `compileSdk 37.1`) directly.
- Do not maintain backward compatibility considerations, legacy aliases, deprecated fallbacks, `Build.VERSION.SDK_INT` guards, or migration layers anywhere.

## State & Persistence

- Keep `UserPreferences` and data models immutable.
- Use `copy()` to generate updated instances.
- Use `PreferencesManager` as the persistent source of truth backed by `SharedPreferences`.
- Persist Health Connect cache entries with metric type, covered date range, and refresh timestamp metadata via `HealthCacheManager`.
- Validate cache metadata before hydrating cached metrics in wallpaper services.

## Live Wallpaper Services

- Extend `BaseWallpaperService` for common surface, visibility, and color-change handling.
- Disable unused offset notifications and touch events via `setOffsetNotificationsEnabled(false)` and `setTouchEventsEnabled(false)`.
- Override `onSurfaceDestroyed` to stop animators, coroutines, and scheduled callbacks.
- Override `onComputeColors` and trigger `notifyColorsChanged` upon palette changes.
- Pause animation scheduling and render a single static frame when `PowerManager.isPowerSaveMode` is active.
- Isolate feature-specific background jobs (such as Health Connect polling) to the owning service (`DayCounterService`).
- Throttle Health Connect refreshes while the wallpaper is visible.

## Graphics & Rendering

- Use `CanvasRenderer` with `ShapeDrawer` strategies and shared `UnitShapeGeometry` paths for all `UnitShape` variants.
- Use `SurfaceHolder.lockHardwareCanvas()` with fallback to software canvas.
- Set surface frame rate hint via `Surface.setFrameRate`.
- Pre-allocate Paint, Path, geometry objects, and `StaticLayout` text layouts during update passes rather than inside `render()`.
- Drive breathing pulse animations through `PulseAnimator` paced at 30 FPS.

## UI & Design

- Build all companion UI in Jetpack Compose using `MaterialExpressiveTheme` (Compose Material 3 1.5.0+ via Compose BOM).
- Prefer stable Material 3 Expressive components (`HorizontalFloatingToolbar`, `SplitButtonLayout`, connected `ToggleButton` groups, `LinearWavyProgressIndicator`, `SliderState`, `TextFieldState`).
- Frame live wallpaper previews on a neutral achromatic mat matching the device aspect ratio.
- Use the bundled `Geist` font family for all text rendering.

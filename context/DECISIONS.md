# Architecture Decisions

Curated record of durable, non-obvious project-level architectural decisions and tradeoffs.

## 2026-11-26 — Pure Jetpack Compose Material 3 Expressive Studio and Android 17 (API 37) Minimum Floor

Context: The companion app previously relied on legacy XML Views (`AppCompat`, `com.google.android.material:material:1.12.0`, `minSdk = 28`, `compileSdk = 34`) with no live wallpaper preview on the main screen. Material Views entered maintenance mode while Jetpack Compose 1.13 + Compose Material 3 1.5.0 (`compose-bom:2026.11.01`) graduated Material 3 Expressive components to stable and requires `compileSdk 37.1`. Furthermore, the app targets modern Android 17 devices (Pixel 10) without legacy compatibility overhead.

Decision: Replace the entire XML companion UI with pure Jetpack Compose (`ComponentActivity`, `StateFlow` UDF) built on `MaterialExpressiveTheme`, `MotionScheme.expressive()`, connected `ToggleButton` groups, `HorizontalFloatingToolbar`, `SplitButtonLayout`, `LinearWavyProgressIndicator`, `PolygonShape`, and stateful `SliderState`. Pin a true device-aspect-ratio live preview on an achromatic neutral mat (to avoid simultaneous-contrast color distortion) above a zero-scroll Palette strip of pure 3-tone Color Orbs rather than miniature dot-grid cards. Raise `minSdk` and `targetSdk` to `37` (`compileSdk 37.1`) to use Dynamic Color, OS-integrated Health Connect, and Android 17's native `Intent.ACTION_OPEN_EYE_DROPPER` without runtime `SDK_INT` branches or legacy fallbacks.

Tradeoff: Drops installation support for devices below Android 17 (API 37), but eliminates AppCompat/MDC-Android/XML layout bloat, removes all runtime API-level branching, and unlocks the full stable Material 3 Expressive 1.5.0 and Android 17 feature set.

Status: active

## 2026-09-06 — Oklch-Based Procedural Theme Generation and Expanded Curated Catalog

Context: The 6-day daily rotation cycle repeated presets too quickly. Generating colors dynamically without strict constraints leads to discordant, low-contrast palettes in non-uniform color spaces (RGB/HSL), while solely hardcoding presets creates maintenance bloat without infinite variety.

Decision: Expand curated rotation to 14 hand-crafted paired themes, and implement a deterministic procedural theme generator (`ProceduralThemeGenerator`) using the perceptually uniform Oklch color space (`OklchColor`). The generator fixes perceptual lightness and chroma invariants (background L 96.5% / 9.5%, past dots L 58%, accent L 62%–72% with golden-angle base hue stepping and complementary offsets) seeded by `LocalDate.toEpochDay()`, producing the dynamic "Atmosphere" theme with evocative naming.

Tradeoff: Requires lightweight color conversions between Oklch, Oklab, and linear sRGB, but eliminates external dependencies, avoids state storage, and guarantees mathematical contrast across all 360 degrees of hue.

## 2026-09-05 — Hardware-Accelerated Rendering, 30 FPS Pacing, and Power-Save Idling

Context: The live wallpaper engine previously utilized software-locked canvas (`SurfaceHolder.lockCanvas()`) running at 60 FPS. On modern high-refresh OLED devices (such as Pixel LTPO panels), software rendering forces expensive CPU-to-GPU framebuffer copies (~600 MB/s memory bandwidth) and keeps LTPO displays running at elevated refresh rates. Furthermore, running animations continuously during system Battery Saver depleted battery on low charge.

Decision: Switch rendering to hardware-accelerated canvas (`SurfaceHolder.lockHardwareCanvas()`) with software fallback, target 30 FPS (`PulseAnimator.TARGET_FPS = 30`), hint display refresh rates via `Surface.setFrameRate(30f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)` on API 30+, and pause frame scheduling entirely (rendering a single static frame at full opacity) when `PowerManager.isPowerSaveMode` is active. Additionally, disable window manager offset and touch IPC deliveries (`setOffsetNotificationsEnabled(false)`, `setTouchEventsEnabled(false)`).

Tradeoff: Frame rate is capped at 30 FPS rather than matching native display refresh rates (60/90/120Hz), but the visual breathing cycle (2000ms duration) is imperceptible from 60 FPS while halving draw calls and allowing LTPO displays to step down to 30Hz, significantly lowering battery consumption.

Status: active

## 2026-08-16 — Paired Light/Dark Theme Model and Deterministic Daily Rotation

Context: The wallpaper color system originally consisted of an ad-hoc collection of disconnected color scheme presets without parity between light and dark modes. Additionally, rotating themes daily needed to work seamlessly across midnight boundaries, device reboots, and configuration changes without storing volatile scheduling state.

Decision: Refactor color schemes into `WallpaperTheme` entities with paired, curated `lightScheme` and `darkScheme` definitions that automatically resolve based on the system's night mode (`Configuration.UI_MODE_NIGHT_YES`). For daily rotation, compute the active theme deterministically via date epoch modulo (`epochDay % rotatableThemes.size`) upon wallpaper initialization and midnight boundary broadcasts.

Tradeoff: Themes must provide balanced light and dark variants rather than standalone arbitrary palettes, but eliminates complex rotation state management and ensures theme consistency across day/night system toggles.

Status: active

## 2026-04-20 — Health Connect Sync Ownership in DayCounterService

Context: Health Connect metrics (Steps, Calories, Distance, Sleep) are exclusive to the Micro (Day Counter) mode. Centralizing health sync in `BaseWallpaperService` bloated the shared base class and introduced unnecessary Health Connect queries for Macro (Life Calendar) mode.

Decision: Isolate Health Connect cache hydration, throttled visible-window refreshes, and midnight boundary syncs directly inside `DayCounterService`, keeping `BaseWallpaperService` completely agnostic of health data.

Tradeoff: `DayCounterService` manages its own coroutine lifecycle and throttled polling loop, but keeps `BaseWallpaperService` and Macro mode decoupled and lightweight.

Status: active

## 2026-04-20 — Health Cache Persistence with Validation Metadata

Context: Querying Health Connect APIs on every frame or lock screen unlock degrades battery life and introduces latency. Persisting raw metrics without cache metadata caused stale or incorrect values to render when users modified their tracked metric or date window.

Decision: Store health aggregates in SharedPreferences via `HealthCacheManager` accompanied by cache metadata (metric type, covered date range, last refresh timestamp), and validate this metadata before hydrating cached metrics.

Tradeoff: Requires invalidation and full backfill logic when metadata mismatches, but avoids redundant background API calls and prevents mismatched metrics from rendering.

Status: active

## 2026-03-13 — Strategy Pattern and Pre-allocation in CanvasRenderer

Context: Live wallpaper rendering runs continuously at up to 60fps on Android Canvas. Instantiating drawing primitives, objects, or branching dynamically for multiple shapes (Circle, Rounded Square, Rhombus) within `onDraw` causes frequent GC pauses and battery consumption.

Decision: Encapsulate shape drawing into dedicated `ShapeDrawer` strategy implementations and pre-calculate all layout coordinates, paths, and paint objects during `onSurfaceChanged` or configuration updates.

Tradeoff: Introduces additional strategy classes compared to a single conditional drawing function, but achieves allocation-free frame rendering during pulse animations.

Status: active

## 2026-03-01 — Direct Font Bundling for Geist Typeface

Context: Perspective - Live relies on precise geometric and numeric alignment for grid overlays and lockscreen visuals across diverse Android OEMs with different system default fonts.

Decision: Bundle the Geist font family directly into core Android resources (`res/font/geist*`) rather than relying on system fallback fonts or Google Fonts downloadable font providers.

Tradeoff: Increases APK distribution size by the size of the font assets, but guarantees consistent typography across all Android OEM skins without network latency or font download failures.

Status: active

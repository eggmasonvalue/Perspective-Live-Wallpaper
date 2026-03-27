# Comprehensive Execution Plan: Health Connect on "Micro"

## 1. Dependencies & Permissions
*   **Action**: Add `androidx.health.connect:connect-client` to `app/build.gradle.kts`.
*   **Action**: Declare `<queries>` for the Health Connect package in `AndroidManifest.xml`.
*   **Action**: Declare `<uses-permission>` tags in `AndroidManifest.xml` for `READ_STEPS`, `READ_TOTAL_CALORIES_BURNED`, `READ_DISTANCE`, `READ_SLEEP`.

## 2. Data & Caching Layer
*   **Action**: Update `UserPreferences` and `PreferencesManager` to store:
    *   `healthMetric` (String: "NONE", "STEPS", "CALORIES", "DISTANCE", "SLEEP")
    *   `healthMetricGoal` (Float, user-defined)
    *   `showStatOverlay` (Boolean)
*   **Action**: Create `HealthCacheManager.kt` to serialize/deserialize a `Map<LocalDate, Float>` (mapping specific dates to their aggregate metric values) in `SharedPreferences` for fast reads.

## 3. Synchronization & Fetching (HealthConnectManager)
*   **Action**: Create `HealthConnectManager.kt` to handle:
    *   Checking permissions for the selected metric.
    *   Fetching aggregate data (`AggregateGroupByPeriodRequest` grouped by `Period.ofDays(1)`).
*   **Action**: Intercept "Set Perspective" button in `MainActivity.kt`. If a health metric is active, run a background sync fetching historical data (from `countdownStartDate` to `today`), update the cache, and *then* start the wallpaper service.
*   **Action**: In `DayCounterService`, on `onVisibilityChanged(true)`, fetch *only today's* metric to keep the active day's progress live, and update the cache.
*   **Action**: Update `performMidnightUpdate` to fetch and finalize *yesterday's* value in the cache, and initialize the renderer for the new day.

## 4. Settings UI Modernization (StyleSelectionBottomSheet & ColorCardAdapter)
*   **Action**: Add the "Daily Health Metrics" section (Metric Selector, Goal Input, Stat Overlay toggle) above the Colors section in `bottom_sheet_style_selection.xml`.
*   **Action**: Update `StyleSelectionBottomSheet.kt` to handle Health Connect permission requests when a metric is selected, and validate the Goal input dynamically.
*   **Action**: Refactor Color Scheme selection (`colorCardsRecyclerView`) into a single horizontally scrollable carousel (`LinearLayoutManager(horizontal)`).
*   **Action**: Order the carousel schemes in `ColorSchemeProvider.kt`: Default (Iconic) -> Health Presets -> Standard Presets -> Custom.
*   **Action**: Add 4 new health-optimized color presets in `ColorSchemeProvider.kt` (Steps Green, Vitality Orange, Distance Purple, Deep Sleep Blue).
*   **Action**: Upgrade the preset thumbnails in `item_color_card.xml` and `ColorCardAdapter.kt` to show a 3x3 grid instead of 4 squares to better represent the final wallpaper.

## 5. Wallpaper Rendering (CanvasRenderer)
*   **Action**: Ensure `CanvasRenderer` safely reads the `healthCache` off the main thread or receives it initialized via `BaseWallpaperService`.
*   **Action**: Map each shape's index to a `LocalDate`.
*   **Action**: **Present Day (Today)**: Keep the exact current visual formatting (the signature breathing shape/color). Draw the stat text (e.g., "5.2k", "7h") perfectly centered *inside* the shape. Dynamically scale the text size based on the shape's bounds.
*   **Action**: **Past Days**: Calculate progress `(cachedValue / healthMetricGoal).coerceIn(0f, 1f)`. Render the past shape, but map its opacity (alpha) based on this progress against the daily goal (e.g., base 20% alpha up to 100% original alpha). Draw the stat text centered inside using the same scaling logic.
*   **Action**: **Future Days**: Render exactly as they are currently (dimmed/empty, no text).
*   **Action**: Add clean, hardcoded suffixes in `formatHealthText` (e.g., `k`, `kcal`, `km/mi`, `h`).

## 6. Documentation & Post-Processing
*   **Action**: Symlink `AGENTS.md` to `GEMINI.md`.
*   **Action**: Update `.context/OVERVIEW.md`, `.context/ARCHITECTURE.md`, `.context/DESIGN.md`, and `.context/CHANGELOG.md`.
*   **Action**: Update `README.md` to document the feature.

# Changelog

## [Unreleased]
### Added
-   **Health Connect**: The "Micro" (Day Counter) mode now integrates with Android Health Connect to display daily aggregate metrics (Steps, Calories, Distance, Sleep) as varying opacities and subtle text overlays for past days, while retaining the signature breathing animation for the current day.
-   **Color Presets**: Added four new health-optimized color schemes (Steps Green, Vitality Orange, Distance Purple, Deep Sleep Blue) to the single unified horizontal carousel.
-   **Runtime Health Sync**: `DayCounterService` now refreshes Health Connect data while visible on a throttled cadence and performs a midnight closeout sync without requiring the settings screen to relaunch the wallpaper.
-   **Health Cache Metadata**: Health cache persistence now tracks metric, covered date range, and last successful refresh time so stale or mismatched snapshots can be invalidated safely.

### Fixed
-   **CI/CD**: Fixed GitHub Actions release workflow permissions to resolve 403 error.
-   **Code Health**: Removed unused `LayoutInflater` import in `ColorPickerDialog`.
-   **Health Updates**: Fixed stale day-counter health data by moving fetch ownership from settings-launch time into the wallpaper service runtime.
-   **Date Mapping**: Fixed day-counter dynamic modes so rendered item dates use the same effective start date as the countdown logic, keeping Health Connect lookups aligned.
-   **Midnight Updates**: Fixed the generic midnight receiver to respect the active wallpaper mode's preference readiness instead of assuming life-calendar preferences.
-   **Settings Scope**: Hid Health Connect controls from the Macro style flow so the UI matches the day-counter-only runtime feature.

## [v1.1.0]

### Added
-   **Typography**: Updated entire app to use **Geist** typeface.
-   **Color Schemes**: Introduced "Iconic" color scheme as default.
-   **Container Padding**: New slider in styling options to adjust grid margins.
-   **Rhombus Shape**: Replaced "Square" option with a Rhombus (rotated square).
-   **Material You**: Wallpaper now exposes colors to the system theme engine.
-   **CI/CD**: GitHub Actions workflow to build and release signed APKs on tags.
-   **License**: Added MIT License.

### Changed
-   **Project Name**: Renamed from "Horizon" to "**Perspective - Live**".
-   **Terminology**: Renamed "Perspective" tab to "**Macro**" (Life Calendar) and "Momentum" tab to "**Micro**" (Day Counter).
-   **Architecture**: Refactored `MainActivity` to use `SettingsViewModel` (MVVM).
-   **Logic**: "No Tomorrow" and "Vs Yesterday" modes now use dynamic date calculations (`DayCounterMode`), fixing the static date bug.
-   **Build**: Updated signing config to use environment variables with debug fallback.

### Fixed
-   **Crash**: Fixed crash when starting screen recording by handling `onSurfaceDestroyed`.
-   **Logic**: Fixed `DayCounterModule` to correctly handle daily updates for presets.

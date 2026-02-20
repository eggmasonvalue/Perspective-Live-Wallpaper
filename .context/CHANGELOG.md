# Changelog

## [Unreleased]

### Fixed
-   **CI/CD**: Fixed GitHub Actions release workflow permissions to resolve 403 error.

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

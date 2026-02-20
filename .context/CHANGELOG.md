# Changelog

## [Unreleased]

### Added
-   **Container Padding**: New slider in styling options to adjust grid margins.
-   **Rhombus Shape**: Replaced "Square" option with a Rhombus (rotated square).
-   **Material You**: Wallpaper now exposes colors to the system theme engine.
-   **CI/CD**: GitHub Actions workflow to build and release signed APKs on tags.
-   **License**: Added MIT License.

### Changed
-   **Architecture**: Refactored `MainActivity` to use `SettingsViewModel` (MVVM).
-   **Logic**: "No Tomorrow" and "Vs Yesterday" modes now use dynamic date calculations (`DayCounterMode`), fixing the static date bug.
-   **Build**: Updated signing config to use environment variables with debug fallback.

### Fixed
-   **Crash**: Fixed crash when starting screen recording by handling `onSurfaceDestroyed`.
-   **Logic**: Fixed `DayCounterModule` to correctly handle daily updates for presets.

# Conventions

## Code Style
-   **Language**: Kotlin (Official style).
-   **Formatting**: Standard Android/Kotlin formatting (4 spaces indent).
-   **Linting**: Use **Detekt** for static analysis.

## Architecture
-   **MVVM**: Use `ViewModel` for UI logic.
-   **Separation of Concerns**: Logic in `modules`, rendering in `rendering`, persistence in `data`.
-   **Dependency Injection**: Manual DI via Factories (e.g., `SettingsViewModelFactory`).

## State Management
-   **Immutability**: `UserPreferences` is immutable. `copy()` is used for updates.
-   **Source of Truth**: `SharedPreferences` (via `PreferencesManager`) is the persistent source of truth.

## UI
-   **Material Design 3**: Use Material components.
-   **Typography**: Use **Geist** font for all text.
-   **XML Layouts**: Keep layouts flat where possible.

## Wallpaper Service
-   **Lifecycle**: Handle `onCreate`, `onSurfaceChanged`, `onVisibilityChanged`, `onSurfaceDestroyed`.
-   **Safety**: Always override `onSurfaceDestroyed` to stop animations.
-   **Material You**: Override `onComputeColors` (API 27+) and call `notifyColorsChanged`.

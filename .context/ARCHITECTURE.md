# Architecture

## High-Level Structure

The application is split into two main components:
1.  **Settings UI**: An Activity-based UI for configuring the wallpaper.
2.  **Wallpaper Services**: Background services that render the live wallpaper on the home/lock screen.

## Module Structure

-   **`com.timehorizons.wallpaper`**
    -   **`data`**: Data models and persistence.
        -   `UserPreferences`: Immutable data class holding settings.
        -   `PreferencesManager`: SharedPreferences wrapper.
        -   `DayCounterMode`: Constants for day counter logic.
        -   `LifeState`: Calculated state for rendering.
    -   **`modules`**: Business logic for counting.
        -   `CountdownModule`: Interface.
        -   `DayCounterModule`: Logic for Momentum tab (days).
        -   `LifeCalendarModule`: Logic for Perspective tab (years).
    -   **`rendering`**: Graphics engine.
        -   `CanvasRenderer`: Draws the grid using Android Canvas.
        -   `GridCalculator`: Computes grid layout (rows/cols) based on screen size and density.
        -   `PulseAnimator`: Handles the "breathing" animation.
    -   **`service`**: Android WallpaperServices.
        -   `LifeCalendarService`: Service for Life Calendar mode.
        -   `DayCounterService`: Service for Day Counter mode.
        -   `MidnightScheduler`: Schedules daily updates.
    -   **`settings`**: UI Layer (MVVM).
        -   `MainActivity`: Host activity.
        -   `SettingsViewModel`: Manages UI state and interacts with `PreferencesManager`.
        -   `StyleSelectionBottomSheet`: UI for customizing look.

## Data Flow

1.  **User Input**: User interacts with `MainActivity` / `StyleSelectionBottomSheet`.
2.  **ViewModel**: `SettingsViewModel` receives actions, updates `LiveData`, and calls `PreferencesManager`.
3.  **Persistence**: `PreferencesManager` saves to `SharedPreferences`.
4.  **Service Update**:
    -   On `onResume` or visibility change, the Service reads `PreferencesManager`.
    -   Or `MidnightScheduler` triggers a broadcast which the Service receives.
5.  **Rendering**: Service initializes `CanvasRenderer` with new `UserPreferences`. `CanvasRenderer` draws to `SurfaceHolder`.

## Key Patterns
-   **MVVM**: Used in the Settings UI.
-   **Observer**: Services listen for broadcasts; UI observes LiveData.
-   **Factory**: `SettingsViewModelFactory` for dependency injection.

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
        -   `GridState`: Calculated state for rendering (replaces `LifeState`).
    -   **`modules`**: Business logic for counting.
        -   `CountdownModule`: Interface.
        -   `DayCounterModule`: Logic for Momentum tab (days).
        -   `LifeCalendarModule`: Logic for Perspective tab (years).
    -   **`rendering`**: Graphics engine.
        -   `CanvasRenderer`: Draws the grid using Android Canvas with optimized ShapeDrawer strategy.
        -   `GridCalculator`: Computes grid layout (rows/cols) based on screen size and density.
        -   `PulseAnimator`: Handles the "breathing" animation.
    -   **`service`**: Android WallpaperServices.
        -   `BaseWallpaperService`: Abstract base class encapsulating common lifecycle and rendering logic.
        -   `LifeCalendarService`: Service for Life Calendar mode (extends BaseWallpaperService).
        -   `DayCounterService`: Service for Day Counter mode (extends BaseWallpaperService).
        -   `MidnightScheduler`: Schedules daily updates via AlarmManager and BroadcastReceiver.
    -   **`settings`**: UI Layer (MVVM).
        -   `MainActivity`: Host activity.
        -   `SettingsViewModel`: Manages UI state and interacts with `PreferencesManager`.
        -   `StyleSelectionBottomSheet`: UI for customizing look.

## Data Flow

1.  **User Input**: User interacts with `MainActivity` / `StyleSelectionBottomSheet`.
2.  **ViewModel**: `SettingsViewModel` receives actions, updates `LiveData`, and calls `PreferencesManager`.
3.  **Persistence**: `PreferencesManager` saves to `SharedPreferences`.
4.  **Service Update**:
    -   On `onVisibilityChanged`, the Service reads `PreferencesManager` and initializes/updates renderer.
    -   `MidnightScheduler` triggers a broadcast at midnight. `BaseWallpaperService` receives it and delegates to `performMidnightUpdate` (e.g., checking for birthday or just refreshing).
5.  **Rendering**: `CanvasRenderer` draws to `SurfaceHolder` using `GridState` and pre-calculated layout.

## Key Patterns
-   **MVVM**: Used in the Settings UI.
-   **Observer**: Services listen for broadcasts; UI observes LiveData.
-   **Template Method**: `BaseWallpaperService` defines the skeleton of wallpaper lifecycle, subclasses provide specific steps (`getGridState`, `performMidnightUpdate`).
-   **Strategy**: `CanvasRenderer` uses `ShapeDrawer` strategies for optimized drawing.

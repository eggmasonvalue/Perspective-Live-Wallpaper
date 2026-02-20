# Architecture

## High-Level Structure

The application is split into two main components:
1.  **Settings UI**: An Activity-based UI for configuring the wallpaper (Macro and Micro modes).
2.  **Wallpaper Services**: Background services that render the live wallpaper on the home/lock screen.

## Module Structure

-   **`com.timehorizons.wallpaper`**
    -   **`data`**: Data models and persistence.
        -   `UserPreferences`: Immutable data class holding settings.
        -   `PreferencesManager`: SharedPreferences wrapper.
        -   `GridState`: Calculated state for rendering.
        -   `GridConfig`: Defines layout properties (rows, columns, size, spacing).
        -   `ColorScheme`, `ColorSchemeProvider`, `CustomColorScheme`: Handle color palettes (Iconic, System, Custom).
        -   `DayCounterMode`: Constants for day counter logic.
    -   **`modules`**: Business logic for counting.
        -   `CountdownModule`: Interface for counting logic.
        -   `DayCounterModule`: Logic for Micro tab (days).
        -   `LifeCalendarModule`: Logic for Macro tab (years).
        -   `ModuleRegistry`: Registry for accessing modules (primarily for Life Calendar).
    -   **`rendering`**: Graphics engine.
        -   `CanvasRenderer`: Draws the grid using Android Canvas with optimized ShapeDrawer strategy.
        -   `PulseAnimator`: Handles the "breathing" animation.
    -   **`service`**: Android WallpaperServices.
        -   `BaseWallpaperService`: Abstract base class encapsulating common lifecycle and rendering logic.
        -   `LifeCalendarService`: Service for Macro mode (extends BaseWallpaperService). Uses `GridState.calculate` which delegates to `LifeCalendarModule`.
        -   `DayCounterService`: Service for Micro mode (extends BaseWallpaperService). Uses `DayCounterModule` directly.
        -   `MidnightScheduler`: Schedules daily updates via AlarmManager and BroadcastReceiver.
    -   **`settings`**: UI Layer (MVVM).
        -   `MainActivity`: Host activity.
        -   `SettingsViewModel`: Manages UI state and interacts with `PreferencesManager`.
        -   `StyleSelectionBottomSheet`: UI for customizing look (Shapes, Colors, Size).
    -   **`utils`**: Utility classes.
        -   `ColorUtils`: Helper for color manipulation.
        -   `DateCalculator`: Helper for date calculations.

## Data Flow

1.  **User Input**: User interacts with `MainActivity` / `StyleSelectionBottomSheet`.
2.  **ViewModel**: `SettingsViewModel` receives actions, updates `LiveData`, and calls `PreferencesManager`.
3.  **Persistence**: `PreferencesManager` saves to `SharedPreferences`.
4.  **Service Update**:
    -   On `onVisibilityChanged`, the Service reads `PreferencesManager` and initializes/updates renderer.
    -   `MidnightScheduler` triggers a broadcast at midnight. `BaseWallpaperService` receives it and delegates to `performMidnightUpdate`.
    -   **Macro (Life Calendar)**: Checks if it's the user's birthday to add a dot.
    -   **Micro (Day Counter)**: Updates the count daily.
5.  **Rendering**: `CanvasRenderer` draws to `SurfaceHolder` using `GridState` and pre-calculated layout.

## Key Patterns
-   **MVVM**: Used in the Settings UI.
-   **Observer**: Services listen for broadcasts; UI observes LiveData.
-   **Template Method**: `BaseWallpaperService` defines the skeleton of wallpaper lifecycle, subclasses provide specific steps (`getGridState`, `performMidnightUpdate`).
-   **Strategy**: `CanvasRenderer` uses `ShapeDrawer` strategies for optimized drawing.

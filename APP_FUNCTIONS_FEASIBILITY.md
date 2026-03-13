# Feasibility Report: Android 16 AppFunctions Implementation

## Executive Summary
This report evaluates the feasibility of integrating **AppFunctions**, a newly introduced experimental feature in Android 16 (API 36). AppFunctions enable Android apps to expose specific, targeted functionality that system-level AI agents (like Google Gemini) and other authorized callers can discover and execute directly via natural language prompts. This is analogous to the Model Context Protocol (MCP) used for server-side tool orchestration, but operates locally on-device.

For **Perspective - Live**, AppFunctions offer a seamless way for users to interact with their live wallpapers—updating their "Life Calendar" or "Day Counter" goals completely hands-free using Gemini.

---

## 1. What are AppFunctions?
AppFunctions act as tools that AI assistants can use to complete tasks without the user needing to open the app and manually navigate the UI.

**How it works:**
1. **Definition:** Developers define functions with specific metadata using the Jetpack `androidx.appfunctions` library and annotate them with `@AppFunction`.
2. **Indexing:** At compile-time, an annotation processor (KSP) generates the necessary XML schemas and Kotlin boilerplate. At runtime, the Android OS indexes these capabilities.
3. **Execution:** An AI agent interprets a user's natural language request (e.g., "Set my Perspective day counter to my vacation on July 15th"), resolves it to the specific app function, extracts the parameters from the prompt, and invokes the function directly.

---

## 2. Technical Requirements
To support AppFunctions, the project must undergo several build and dependency updates:

*   **SDK Target:** The application must be compiled with `compileSdk = 36` (Android 16 Developer Preview/Beta SDK). The current project uses `compileSdk = 34`.
*   **Dependencies:**
    *   `androidx.appfunctions:appfunctions:1.0.0-alphaXX`
    *   `androidx.appfunctions:appfunctions-service:1.0.0-alphaXX`
*   **Compiler Plugin:** The project must adopt KSP (Kotlin Symbol Processing) and apply the AppFunctions compiler plugin:
    *   `androidx.appfunctions:appfunctions-compiler:1.0.0-alphaXX`
*   **Permissions:** The caller (e.g., Gemini) requires the `EXECUTE_APP_FUNCTIONS` system permission.
*   **Device:** A physical or emulated device running Android 16+ is required for testing.

---

## 3. Potential Use Cases for Perspective - Live

The dual nature of the wallpaper engine (Macro / Micro) makes it a strong candidate for AppFunction integration.

### Use Case 1: Updating the Day Counter (Micro Mode)
*   **User Prompt:** *"Hey Google, start a countdown to my Tokyo trip on October 12th in Perspective."*
*   **Function Execution:** An AppFunction (e.g., `setCustomDayCounter`) takes parameters `eventName` (Tokyo trip) and `eventDate` (2026-10-12). The function updates the `UserPreferences` via `PreferencesManager` and optionally sends an intent to refresh the wallpaper service.

### Use Case 2: Switching Day Counter Modes
*   **User Prompt:** *"Set my Perspective wallpaper to 'No Tomorrow' mode."*
*   **Function Execution:** An AppFunction (e.g., `setDayCounterMode`) receives the target mode enum/string (`NO_TOMORROW`) and updates the preferences. The live wallpaper automatically re-renders based on the `SharedPreferences.OnSharedPreferenceChangeListener` in `BaseWallpaperService`.

### Use Case 3: Updating Life Calendar (Macro Mode)
*   **User Prompt:** *"Update my expected lifespan to 100 years in Perspective."*
*   **Function Execution:** An AppFunction updates the `expectedLifespan` in `UserPreferences`.

### Use Case 4: Changing Visual Styles
*   **User Prompt:** *"Change my Perspective live wallpaper shape to Rhombus."*
*   **Function Execution:** An AppFunction (e.g., `updateStyle`) changes the `unitShapeId` to `"rhombus"`.

---

## 4. Implementation Strategy

To implement this, the following changes to the codebase would be required:

1.  **Update `build.gradle.kts`:**
    *   Bump `compileSdk` and `targetSdk` to 36.
    *   Add KSP plugin.
    *   Add `androidx.appfunctions` dependencies.

2.  **Create an AppFunctions Service/Receiver:**
    *   The Jetpack library relies on a service or receiver to handle the incoming invocation intents from the OS.
    *   Example interface:
        ```kotlin
        @AppFunction
        fun setDayCounterTarget(
            @AppFunctionParameter(description = "The name of the event") eventName: String,
            @AppFunctionParameter(description = "The date of the event in YYYY-MM-DD format") eventDateString: String
        ) {
            val date = LocalDate.parse(eventDateString)
            preferencesManager.updateEventName(eventName)
            preferencesManager.updateEventDate(date)
            preferencesManager.updateDayCounterMode(DayCounterMode.STATIC)
            // The existing SharedPreferenceChangeListener in BaseWallpaperService
            // will automatically catch this and redraw the canvas.
        }
        ```

3.  **Manifest Updates:**
    *   The KSP plugin will generate XML metadata that needs to be referenced in the `AndroidManifest.xml` (though the plugin usually handles merging this automatically).

---

## 5. Feasibility Assessment & Risks

### Feasibility: **Moderate to High (for prototyping), Low (for production)**

**Why?**
*   **Pros:** The architecture of *Perspective - Live* is perfectly suited for AppFunctions. Because the rendering engine (`BaseWallpaperService`) already aggressively listens to `SharedPreferences` changes and re-renders on the fly, any external tool that modifies these preferences will immediately update the live wallpaper without requiring explicit IPC or complex service bindings.
*   **Cons (Risks):**
    *   **Extremely Early Stage:** As of March 2026, AppFunctions is an experimental alpha feature. APIs are subject to breaking changes.
    *   **Gemini Resolution Bugs:** Developer community reports (e.g., GitHub Issue #1151) indicate that while functions are indexed successfully by the Android OS (`dumpsys app_function`), Gemini currently struggles to resolve custom function metadata or execute them reliably in third-party apps.
    *   **SDK Limitations:** Updating to `compileSdk = 36` may introduce compatibility issues with existing dependencies or require updating AGP/Gradle versions that the project might not be ready for.

### Recommendation
**Do not merge into production immediately.**

Instead, create a prototype/spike branch (`feature/app-functions-spike`). Implement the SDK 36 upgrade and a single AppFunction (e.g., `setDayCounterTarget`). Test it on an Android 16 emulator or device with the latest Gemini assistant. Monitor the Jetpack AppFunctions issue tracker until Google resolves the Gemini discovery bugs for custom third-party schemas.

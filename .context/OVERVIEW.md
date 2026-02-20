# Project Overview

**Perspective - Live** is a minimalist Live Wallpaper engine for Android that transforms abstract time into a tangible visual landscape. It features a "Macro" (Life Calendar) and a "Micro" (Day Counter) mode.

## Core Features
- **Macro (Life Calendar)**: Visualizes life in years on a grid.
- **Micro (Day Counter)**: Visualizes days until an event or "No Tomorrow" mode.
- **Deep Customization**: Shapes (Circle, Rounded Square, Rhombus), Density, Padding, Colors (Iconic Scheme and more).
- **Material You**: Adapts to system colors (Monet).
- **Typography**: Uses the **Geist** typeface for a modern, crisp look.
- **Performance**: Zero battery drain when not visible; native Canvas rendering.

## Tech Stack
- **Language**: Kotlin
- **Platform**: Android (Min SDK 28, Target SDK 34)
- **Architecture**: MVVM (Settings), Android Services (Wallpaper)
- **UI**: Android Views, Material Design 3
- **CI/CD**: GitHub Actions (Release builds)

## Dependencies
- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `androidx.recyclerview:recyclerview`
- `androidx.lifecycle:lifecycle-viewmodel-ktx`
- `androidx.lifecycle:lifecycle-livedata-ktx`
- `androidx.activity:activity-ktx`
- `junit:junit:4.13.2` (Test)
- `io.gitlab.arturbosch.detekt` (Linting)

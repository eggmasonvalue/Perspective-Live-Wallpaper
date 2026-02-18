# Project Overview

**Horizon** is a minimalist Live Wallpaper engine for Android that transforms abstract time into a tangible visual landscape. It features a "Life Calendar" (Perspective) and a "Day Counter" (Momentum) mode.

## Core Features
- **Perspective (Life Calendar)**: Visualizes life in years on a grid.
- **Momentum (Day Counter)**: Visualizes days until an event or "No Tomorrow" mode.
- **Deep Customization**: Shapes (Circle, Rounded Square, Rhombus), Density, Padding, Colors.
- **Material You**: Adapts to system colors (Monet).
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

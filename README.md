# Perspective - Live 🌅

**Perspective - Live** is a minimalist Live Wallpaper engine for Android that transforms abstract time into a tangible visual landscape. Designed for those who live with intention, ambition, and a clear vision of the future.

[![Website](https://img.shields.io/badge/website-perspective--live-orange.svg)](https://eggmasonvalue.github.io/Perspective-Live-Wallpaper/)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)
![Typography](https://img.shields.io/badge/typeface-Geist-black.svg)

**[Visit the Website for a Live Demo](https://eggmasonvalue.github.io/Perspective-Live-Wallpaper/)**

## 🌌 The Concept

Time famously flies. **Perspective - Live** renders time as a precise, modular grid of geometry, delivering a frequent dose of perspective at a glance.

### 🔭 Macro (Life Calendar)

Your life clock on your lock screen.

- **The Grid**: Each unit represents **one year**.
- **The Goal**: Visualize how much life you have lived versus what you might have left.

### 🚀 Micro (Day Counter)

Focus energy on the immediate future.

- **The Grid**: Each unit represents **one day**.
- **The Modes**:
  - **"There is no tomorrow"**: Single visible shape for today (breathing animation).
  - **"You vs. You (yesterday)"**: Two visible shapes — one for today (breathing) and one for yesterday.
  - **"This Year"**: Auto-rolling current calendar year grid in **Days** (365/366) or **52 Weeks**, resetting automatically every January 1.
  - **"Custom"**: Goal and deadline tracking across custom date ranges.
- **Health Connect Integration**: Link daily physical effort directly into the grid.
  - **Metrics**: Track **Steps**, **Calories**, **Distance**, or **Sleep**.
  - **Visualization**: Past days automatically map fill opacity to goal progress while the current day pulses. An optional typographic overlay displays raw metric values inside the shapes.

## ✨ Features

### 🎨 Deep Customization

Customize the aesthetic via the built-in **Style** sheet:

- **Shape Shifting**: Choose from 12 architectural and Material 3 Expressive shapes — **Circle**, **Rounded**, **Squircle**, **Square**, **Rhombus**, **Hexagon**, **Octagon**, **Capsule**, **Clover**, **Scallop**, **Leaf**, and **Spark**.
- **Density Control**: Adjust grid scale from **0.5x** (airy, minimalist) to **1.0x** (dense, data-rich).
- **Container Padding**: Fine-tune independent horizontal, top, and bottom margins around the grid for lock-screen clocks, docks, and device bezels.
- **Colors**: 14 curated paired themes (*Iconic*, *Nordic Minimal*, *Warm Sand*, *Glacial Peak*, *Rose Quartz*, *Monochrome Zen*, *Sumi & Cinnabar*, *Boreal Forest*, *Sahara Dunes*, *Kyoto Celadon*, *Alpenglow*, *Basalt & Ochre*, *Pacific Drift*, *Terracotta Courtyard*), the generative *Atmosphere* theme producing unique daily palettes via perceptual Oklch color math, and dedicated Health Connect presets.
- **Daily Rotation**: Automatically cycle through rotatable color schemes daily at midnight over a 14-day cycle, or select *Atmosphere* for infinite, unrepeatable daily procedural palettes.
- **Material You**: Dynamic wallpaper color extraction on supported Android versions.

### ✒️ Typography

- **Geist**: Bundled directly into core resources to ensure crisp typography for health metrics and overlays across devices.

### ⚡ Performance

- **Zero Idle Drain**: Rendering engine pauses completely when the wallpaper is not visible.
- **Native Rendering**: Canvas-based rendering with pre-allocated geometry for fluid animations.

## 🛠️ Usage

1. Download the APK from the [Releases](https://github.com/eggmasonvalue/Perspective-Live-Wallpaper/releases/latest) page (requires Android 17+).
2. Open the **Perspective - Live** studio app.
3. Switch between **Macro** and **Micro** in the top bar while viewing the live true-aspect preview (tap the preview to inspect 1:1 fullscreen).
4. Use the floating bottom toolbar to configure **Horizon**, **Palette** (3-tone Color Orbs, *Atmosphere*, or **Custom Aura** with Android 17 EyeDropper), and **Craft** (shape, scale, independent horizontal/top/bottom margins, breathing tempo).
5. Tap **Set** on the floating split button to apply the live wallpaper, or **Preview** (when active) to open Pixel's **Wallpaper & style** lock/home screen preview.

## 💻 Development & Building

### Prerequisites

- Android SDK (`compileSdk 37.1`, `minSdk 37`, `targetSdk 37`)
- JDK 17+

### Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Run Static Analysis (Detekt)
./gradlew detekt

# Run Documentation Linter
npx markdownlint-cli2 "**/*.md"
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*The problem is that you think you have time.
                    - Gautama Buddha*

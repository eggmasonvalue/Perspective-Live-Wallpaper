#!/bin/bash
sed -i 's/val textWidthPadding = 0.9f/val TEXT_WIDTH_PADDING = 0.9f\n            val textWidthPadding = TEXT_WIDTH_PADDING/' app/src/main/kotlin/com/perspectivelive/wallpaper/rendering/CanvasRenderer.kt

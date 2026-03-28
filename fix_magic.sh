#!/bin/bash
sed -i 's/val textWidth = (p.size \* 0.9f).toInt()/val textWidthPadding = 0.9f\n            val textWidth = (p.size * textWidthPadding).toInt()/' app/src/main/kotlin/com/perspectivelive/wallpaper/rendering/CanvasRenderer.kt

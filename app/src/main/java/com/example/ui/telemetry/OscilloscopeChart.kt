package com.example.ui.telemetry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun OscilloscopeChart(
    xHistory: List<Float>,
    yHistory: List<Float>,
    zHistory: List<Float>,
    minVal: Float,
    maxVal: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070B12))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val range = (maxVal - minVal).coerceAtLeast(0.001f)

            // Grid lines
            val gridColor = Color(0x1A00F5D4)
            val centerLineColor = Color(0x3300F5D4)

            drawLine(
                color = centerLineColor,
                start = Offset(0f, centerY),
                end = Offset(w, centerY),
                strokeWidth = 1f
            )

            for (i in 1..3) {
                val y1 = centerY - (h * 0.25f * (i / 2f))
                val y2 = centerY + (h * 0.25f * (i / 2f))
                drawLine(color = gridColor, start = Offset(0f, y1), end = Offset(w, y1), strokeWidth = 1f)
                drawLine(color = gridColor, start = Offset(0f, y2), end = Offset(w, y2), strokeWidth = 1f)
            }

            fun drawSeries(values: List<Float>, color: Color) {
                if (values.size < 2) return
                val stepX = w / (values.size - 1).coerceAtLeast(1)
                val path = Path()

                for (i in values.indices) {
                    val normalized = ((values[i] - minVal) / range).coerceIn(0f, 1f)
                    val px = i * stepX
                    val py = h - normalized * h

                    if (i == 0) {
                        path.moveTo(px, py)
                    } else {
                        path.lineTo(px, py)
                    }
                }

                drawPath(path = path, color = color, style = Stroke(width = 2f))
            }

            // Draw series X (Red/Pink), Y (Green), Z (Cyan/Blue)
            drawSeries(xHistory, Color(0xFFFF0055))
            drawSeries(yHistory, Color(0xFF00F5D4))
            drawSeries(zHistory, Color(0xFFFFD166))
        }
    }
}

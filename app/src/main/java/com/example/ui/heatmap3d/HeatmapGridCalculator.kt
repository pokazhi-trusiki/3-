package com.example.ui.heatmap3d

import androidx.compose.ui.graphics.Color
import com.example.data.SensorLogPoint
import com.example.sensor.ColorPalette
import com.example.sensor.HeatmapMetric
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class GridCell(
    val gridX: Int,
    val gridY: Int,
    val worldX: Float, // meters
    val worldY: Float, // meters
    val value: Float,
    val normalizedValue: Float, // 0.0 .. 1.0
    val elevationHeight: Float // scaled for 3D display
)

data class HeatmapGridData(
    val cells: List<GridCell>,
    val gridCols: Int,
    val gridRows: Int,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minValue: Float,
    val maxValue: Float,
    val rawPoints: List<SensorLogPoint>
)

object HeatmapGridCalculator {

    fun interpolateColor(palette: ColorPalette, fraction: Float): Color {
        val clamped = fraction.coerceIn(0f, 1f)
        val colors = palette.colors
        if (colors.isEmpty()) return Color.Cyan
        if (colors.size == 1) return colors.first()

        val segment = 1.0f / (colors.size - 1)
        val index = (clamped / segment).toInt().coerceIn(0, colors.size - 2)
        val localFraction = ((clamped - index * segment) / segment).coerceIn(0f, 1f)

        val startColor = colors[index]
        val endColor = colors[index + 1]

        return Color(
            red = startColor.red + (endColor.red - startColor.red) * localFraction,
            green = startColor.green + (endColor.green - startColor.green) * localFraction,
            blue = startColor.blue + (endColor.blue - startColor.blue) * localFraction,
            alpha = startColor.alpha + (endColor.alpha - startColor.alpha) * localFraction
        )
    }

    fun computeGrid(
        points: List<SensorLogPoint>,
        metric: HeatmapMetric,
        gridResolution: Int = 18,
        heightScaleFactor: Float = 1.5f
    ): HeatmapGridData {
        if (points.isEmpty()) {
            return HeatmapGridData(
                cells = emptyList(),
                gridCols = 0,
                gridRows = 0,
                minX = -10f,
                maxX = 10f,
                minY = -10f,
                maxY = 10f,
                minValue = 0f,
                maxValue = 1f,
                rawPoints = emptyList()
            )
        }

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minVal = Float.MAX_VALUE
        var maxVal = -Float.MAX_VALUE

        val pointValues = ArrayList<Float>(points.size)

        for (p in points) {
            val v = extractMetricValue(p, metric)
            pointValues.add(v)
            if (p.relX < minX) minX = p.relX
            if (p.relX > maxX) maxX = p.relX
            if (p.relY < minY) minY = p.relY
            if (p.relY > maxY) maxY = p.relY
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }

        // Add padding to bounding box
        val padding = max(3f, max(maxX - minX, maxY - minY) * 0.15f)
        minX -= padding
        maxX += padding
        minY -= padding
        maxY += padding

        if (maxVal <= minVal) {
            maxVal = minVal + 1f
        }

        val cols = gridResolution
        val rows = gridResolution
        val stepX = (maxX - minX) / (cols - 1).coerceAtLeast(1)
        val stepY = (maxY - minY) / (rows - 1).coerceAtLeast(1)

        val cells = ArrayList<GridCell>(cols * rows)
        val influenceRadius = max(stepX, stepY) * 2.8f
        val radiusSq = influenceRadius * influenceRadius

        for (gy in 0 until rows) {
            val wy = minY + gy * stepY
            for (gx in 0 until cols) {
                val wx = minX + gx * stepX

                var weightSum = 0f
                var weightedVal = 0f

                // Inverse Distance Weighting with Gaussian decay
                for (i in points.indices) {
                    val p = points[i]
                    val dx = wx - p.relX
                    val dy = wy - p.relY
                    val distSq = dx * dx + dy * dy

                    if (distSq < radiusSq) {
                        val weight = exp(-distSq / (2f * (influenceRadius * 0.5f) * (influenceRadius * 0.5f)))
                        weightSum += weight
                        weightedVal += weight * pointValues[i]
                    }
                }

                val finalVal = if (weightSum > 0.0001f) {
                    weightedVal / weightSum
                } else {
                    minVal
                }

                val normalized = ((finalVal - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                val elevHeight = normalized * 40f * heightScaleFactor

                cells.add(
                    GridCell(
                        gridX = gx,
                        gridY = gy,
                        worldX = wx,
                        worldY = wy,
                        value = finalVal,
                        normalizedValue = normalized,
                        elevationHeight = elevHeight
                    )
                )
            }
        }

        return HeatmapGridData(
            cells = cells,
            gridCols = cols,
            gridRows = rows,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minValue = minVal,
            maxValue = maxVal,
            rawPoints = points
        )
    }

    fun extractMetricValue(point: SensorLogPoint, metric: HeatmapMetric): Float {
        return when (metric) {
            HeatmapMetric.MAGNETIC_FIELD -> point.magMagnitude
            HeatmapMetric.ACCELEROMETER -> point.accelMagnitude
            HeatmapMetric.GYROSCOPE -> point.gyroMagnitude
            HeatmapMetric.AMBIENT_LIGHT -> point.lightLux
            HeatmapMetric.BAROMETRIC_PRESSURE -> point.pressureHpa
            HeatmapMetric.GPS_SPEED -> point.speedKmh
            HeatmapMetric.COMPOSITE_STRESS -> {
                val accelNorm = (point.accelMagnitude - 9.8f).coerceAtLeast(0f) * 4f
                val magNorm = (point.magMagnitude - 45f).coerceAtLeast(0f) * 0.5f
                val gyroNorm = point.gyroMagnitude * 10f
                (accelNorm + magNorm + gyroNorm).coerceIn(0f, 100f)
            }
        }
    }
}

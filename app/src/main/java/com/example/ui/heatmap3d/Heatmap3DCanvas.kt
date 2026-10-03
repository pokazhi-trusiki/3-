package com.example.ui.heatmap3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.SensorLogPoint
import com.example.sensor.ColorPalette
import com.example.sensor.HeatmapMetric
import com.example.sensor.Render3DMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val originalCell: GridCell? = null,
    val originalPoint: SensorLogPoint? = null
)

@Composable
fun Heatmap3DCanvas(
    gridData: HeatmapGridData,
    metric: HeatmapMetric,
    palette: ColorPalette,
    renderMode: Render3DMode,
    cameraYaw: Float,
    cameraPitch: Float,
    cameraZoom: Float,
    panX: Float,
    panY: Float,
    heightScale: Float,
    selectedPoint: SensorLogPoint?,
    onSelectPoint: (SensorLogPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF090D16))
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f

            val boundCx = (gridData.minX + gridData.maxX) / 2f
            val boundCy = (gridData.minY + gridData.maxY) / 2f
            val maxSpan = maxOf(gridData.maxX - gridData.minX, gridData.maxY - gridData.minY, 10f)

            // Base scale in pixels per meter
            val baseScale = (minOf(canvasW, canvasH) * 0.45f / maxSpan) * cameraZoom

            // Convert angles to radians
            val yawRad = Math.toRadians(cameraYaw.toDouble()).toFloat()
            val pitchRad = Math.toRadians(cameraPitch.toDouble()).toFloat()

            val cosYaw = cos(yawRad)
            val sinYaw = sin(yawRad)
            val cosPitch = cos(pitchRad)
            val sinPitch = sin(pitchRad)

            val focalDist = 800f

            fun project3D(wx: Float, wy: Float, wz: Float): ProjectedPoint {
                // Translate relative to center of bounding box
                val dx = (wx - boundCx) * baseScale
                val dy = (wy - boundCy) * baseScale
                val dz = wz * baseScale * heightScale

                // Rotate around Z axis (Yaw)
                val rx = dx * cosYaw - dy * sinYaw
                val ry = dx * sinYaw + dy * cosYaw

                // Rotate around X axis (Pitch)
                val rz = dz * cosPitch - ry * sinPitch
                val py = ry * cosPitch + dz * sinPitch

                // Perspective division
                val dist = focalDist - py
                val depth = py
                val projScale = if (dist > 50f) focalDist / dist else 0.5f

                val sx = centerX + panX + rx * projScale
                val sy = centerY + panY - rz * projScale

                return ProjectedPoint(sx, sy, depth)
            }

            // Draw Holographic Ground Plane & Axis
            drawGroundGrid(
                boundCx, boundCy, maxSpan,
                cosYaw, sinYaw, cosPitch, sinPitch,
                focalDist, centerX, centerY, panX, panY, baseScale
            )

            // Render according to chosen mode
            when (renderMode) {
                Render3DMode.TERRAIN_SURFACE -> {
                    drawTerrainMesh(gridData, palette, ::project3D)
                }
                Render3DMode.VOXEL_COLUMNS -> {
                    drawVoxelPillars(gridData, palette, ::project3D, baseScale)
                }
                Render3DMode.POINT_CLOUD -> {
                    drawPointCloud(gridData, metric, palette, ::project3D, selectedPoint)
                }
                Render3DMode.CONTOUR_2D -> {
                    drawContour2D(gridData, metric, palette, canvasW, canvasH)
                }
            }

            // Draw GPS trajectory ribbon if points exist
            if (renderMode != Render3DMode.CONTOUR_2D && gridData.rawPoints.size > 1) {
                drawPathRibbon(gridData.rawPoints, ::project3D)
            }
        }
    }
}

private fun DrawScope.drawGroundGrid(
    boundCx: Float, boundCy: Float, maxSpan: Float,
    cosYaw: Float, sinYaw: Float, cosPitch: Float, sinPitch: Float,
    focalDist: Float, centerX: Float, centerY: Float, panX: Float, panY: Float, baseScale: Float
) {
    val gridColor = Color(0x2A00F5D4)
    val axisColor = Color(0x6600F5D4)
    val halfSpan = (maxSpan * 0.6f).coerceAtLeast(10f)

    fun projectFloor(wx: Float, wy: Float): Offset? {
        val dx = (wx - boundCx) * baseScale
        val dy = (wy - boundCy) * baseScale
        val rx = dx * cosYaw - dy * sinYaw
        val ry = dx * sinYaw + dy * cosYaw
        val py = ry * cosPitch
        val rz = -ry * sinPitch

        val dist = focalDist - py
        if (dist <= 50f) return null
        val projScale = focalDist / dist
        return Offset(centerX + panX + rx * projScale, centerY + panY - rz * projScale)
    }

    val steps = 8
    val stepSize = (halfSpan * 2f) / steps

    for (i in 0..steps) {
        val coord = -halfSpan + i * stepSize
        // Line parallel to Y
        val p1 = projectFloor(boundCx + coord, boundCy - halfSpan)
        val p2 = projectFloor(boundCx + coord, boundCy + halfSpan)
        if (p1 != null && p2 != null) {
            drawLine(
                color = if (i == steps / 2) axisColor else gridColor,
                start = p1,
                end = p2,
                strokeWidth = if (i == steps / 2) 2f else 1f
            )
        }

        // Line parallel to X
        val q1 = projectFloor(boundCx - halfSpan, boundCy + coord)
        val q2 = projectFloor(boundCx + halfSpan, boundCy + coord)
        if (q1 != null && q2 != null) {
            drawLine(
                color = if (i == steps / 2) axisColor else gridColor,
                start = q1,
                end = q2,
                strokeWidth = if (i == steps / 2) 2f else 1f
            )
        }
    }

    // Compass indicator (North pointer)
    val origin = projectFloor(boundCx, boundCy)
    val northP = projectFloor(boundCx, boundCy + halfSpan * 0.5f)
    if (origin != null && northP != null) {
        drawLine(
            color = Color(0xFFFF0055),
            start = origin,
            end = northP,
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFFFF0055),
            radius = 4f,
            center = northP
        )
    }
}

private fun DrawScope.drawTerrainMesh(
    gridData: HeatmapGridData,
    palette: ColorPalette,
    project: (Float, Float, Float) -> ProjectedPoint
) {
    val cols = gridData.gridCols
    val rows = gridData.gridRows
    if (cols < 2 || rows < 2) return

    val cells = gridData.cells

    // Collect quads with depth for Painter's algorithm (render back-to-front)
    data class Quad(
        val p00: ProjectedPoint,
        val p10: ProjectedPoint,
        val p11: ProjectedPoint,
        val p01: ProjectedPoint,
        val avgDepth: Float,
        val avgNorm: Float
    )

    val quads = ArrayList<Quad>((cols - 1) * (rows - 1))

    for (gy in 0 until rows - 1) {
        for (gx in 0 until cols - 1) {
            val c00 = cells[gy * cols + gx]
            val c10 = cells[gy * cols + (gx + 1)]
            val c11 = cells[(gy + 1) * cols + (gx + 1)]
            val c01 = cells[(gy + 1) * cols + gx]

            val p00 = project(c00.worldX, c00.worldY, c00.elevationHeight)
            val p10 = project(c10.worldX, c10.worldY, c10.elevationHeight)
            val p11 = project(c11.worldX, c11.worldY, c11.elevationHeight)
            val p01 = project(c01.worldX, c01.worldY, c01.elevationHeight)

            val avgDepth = (p00.depth + p10.depth + p11.depth + p01.depth) / 4f
            val avgNorm = (c00.normalizedValue + c10.normalizedValue + c11.normalizedValue + c01.normalizedValue) / 4f

            quads.add(Quad(p00, p10, p11, p01, avgDepth, avgNorm))
        }
    }

    // Sort by depth descending (Painter's algorithm)
    quads.sortByDescending { it.avgDepth }

    for (q in quads) {
        val color = HeatmapGridCalculator.interpolateColor(palette, q.avgNorm)
        val fillColor = color.copy(alpha = 0.55f)
        val wireColor = color.copy(alpha = 0.85f)

        val path = Path().apply {
            moveTo(q.p00.screenX, q.p00.screenY)
            lineTo(q.p10.screenX, q.p10.screenY)
            lineTo(q.p11.screenX, q.p11.screenY)
            lineTo(q.p01.screenX, q.p01.screenY)
            close()
        }

        drawPath(path = path, color = fillColor)
        drawPath(path = path, color = wireColor, style = Stroke(width = 1.2f))
    }
}

private fun DrawScope.drawVoxelPillars(
    gridData: HeatmapGridData,
    palette: ColorPalette,
    project: (Float, Float, Float) -> ProjectedPoint,
    baseScale: Float
) {
    val cells = gridData.cells
    if (cells.isEmpty()) return

    data class Pillar(
        val baseP: ProjectedPoint,
        val topP: ProjectedPoint,
        val norm: Float,
        val depth: Float,
        val radiusPx: Float
    )

    val pillars = ArrayList<Pillar>(cells.size)
    val stepX = if (gridData.gridCols > 1) (gridData.maxX - gridData.minX) / (gridData.gridCols - 1) else 2f
    val rPx = (stepX * 0.42f * baseScale).coerceIn(4f, 26f)

    for (c in cells) {
        val baseP = project(c.worldX, c.worldY, 0f)
        val topP = project(c.worldX, c.worldY, c.elevationHeight)
        pillars.add(Pillar(baseP, topP, c.normalizedValue, baseP.depth, rPx))
    }

    pillars.sortByDescending { it.depth }

    for (p in pillars) {
        val color = HeatmapGridCalculator.interpolateColor(palette, p.norm)
        val colSide = color.copy(alpha = 0.45f)
        val colTop = color.copy(alpha = 0.95f)

        // Draw pillar column
        drawLine(
            color = colSide,
            start = Offset(p.baseP.screenX, p.baseP.screenY),
            end = Offset(p.topP.screenX, p.topP.screenY),
            strokeWidth = p.radiusPx * 1.6f,
            cap = StrokeCap.Round
        )

        // Top cap
        drawCircle(
            color = colTop,
            radius = p.radiusPx * 0.9f,
            center = Offset(p.topP.screenX, p.topP.screenY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = p.radiusPx * 0.4f,
            center = Offset(p.topP.screenX, p.topP.screenY)
        )
    }
}

private fun DrawScope.drawPointCloud(
    gridData: HeatmapGridData,
    metric: HeatmapMetric,
    palette: ColorPalette,
    project: (Float, Float, Float) -> ProjectedPoint,
    selectedPoint: SensorLogPoint?
) {
    val points = gridData.rawPoints
    if (points.isEmpty()) return

    val minV = gridData.minValue
    val maxV = gridData.maxValue
    val range = (maxV - minV).coerceAtLeast(0.001f)

    data class Point3D(
        val pTop: ProjectedPoint,
        val pBase: ProjectedPoint,
        val point: SensorLogPoint,
        val norm: Float,
        val depth: Float
    )

    val list = ArrayList<Point3D>(points.size)

    for (p in points) {
        val v = HeatmapGridCalculator.extractMetricValue(p, metric)
        val norm = ((v - minV) / range).coerceIn(0f, 1f)
        val elev = norm * 45f

        val pTop = project(p.relX, p.relY, elev)
        val pBase = project(p.relX, p.relY, 0f)
        list.add(Point3D(pTop, pBase, p, norm, pTop.depth))
    }

    list.sortByDescending { it.depth }

    for (item in list) {
        val color = HeatmapGridCalculator.interpolateColor(palette, item.norm)
        val isSelected = selectedPoint?.id == item.point.id

        // Vertical stem down to floor
        drawLine(
            color = color.copy(alpha = 0.35f),
            start = Offset(item.pBase.screenX, item.pBase.screenY),
            end = Offset(item.pTop.screenX, item.pTop.screenY),
            strokeWidth = 1.2f
        )
        // Base ground dot
        drawCircle(
            color = Color.DarkGray.copy(alpha = 0.4f),
            radius = 2.5f,
            center = Offset(item.pBase.screenX, item.pBase.screenY)
        )

        // Outer glow
        drawCircle(
            color = color.copy(alpha = if (isSelected) 0.8f else 0.35f),
            radius = if (isSelected) 14f else 8f,
            center = Offset(item.pTop.screenX, item.pTop.screenY)
        )
        // Core beacon
        drawCircle(
            color = color,
            radius = if (isSelected) 7f else 4.5f,
            center = Offset(item.pTop.screenX, item.pTop.screenY)
        )
        drawCircle(
            color = Color.White,
            radius = if (isSelected) 3.5f else 1.8f,
            center = Offset(item.pTop.screenX, item.pTop.screenY)
        )
    }
}

private fun DrawScope.drawPathRibbon(
    points: List<SensorLogPoint>,
    project: (Float, Float, Float) -> ProjectedPoint
) {
    if (points.size < 2) return

    val projectedOffsets = points.map { p ->
        val proj = project(p.relX, p.relY, 2f)
        Offset(proj.screenX, proj.screenY)
    }

    for (i in 0 until projectedOffsets.size - 1) {
        val p1 = projectedOffsets[i]
        val p2 = projectedOffsets[i + 1]
        val progress = i.toFloat() / (points.size - 1)
        val strokeColor = Color(0xFF00F5D4).copy(alpha = 0.4f + progress * 0.5f)

        drawLine(
            color = strokeColor,
            start = p1,
            end = p2,
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    // Highlight current leading position
    val lastOffset = projectedOffsets.last()
    drawCircle(
        color = Color(0xFF00F5D4).copy(alpha = 0.3f),
        radius = 16f,
        center = lastOffset
    )
    drawCircle(
        color = Color(0xFF00F5D4),
        radius = 6f,
        center = lastOffset
    )
    drawCircle(
        color = Color.White,
        radius = 2.5f,
        center = lastOffset
    )
}

private fun DrawScope.drawContour2D(
    gridData: HeatmapGridData,
    metric: HeatmapMetric,
    palette: ColorPalette,
    canvasW: Float,
    canvasH: Float
) {
    val boundCx = (gridData.minX + gridData.maxX) / 2f
    val boundCy = (gridData.minY + gridData.maxY) / 2f
    val maxSpan = maxOf(gridData.maxX - gridData.minX, gridData.maxY - gridData.minY, 10f)

    val scale = minOf(canvasW, canvasH) * 0.85f / maxSpan
    val cx = canvasW / 2f
    val cy = canvasH / 2f

    fun toScreen(wx: Float, wy: Float): Offset {
        val sx = cx + (wx - boundCx) * scale
        val sy = cy - (wy - boundCy) * scale
        return Offset(sx, sy)
    }

    val cols = gridData.gridCols
    val rows = gridData.gridRows
    val cells = gridData.cells

    for (gy in 0 until rows - 1) {
        for (gx in 0 until cols - 1) {
            val c00 = cells[gy * cols + gx]
            val c10 = cells[gy * cols + (gx + 1)]
            val c11 = cells[(gy + 1) * cols + (gx + 1)]
            val c01 = cells[(gy + 1) * cols + gx]

            val s00 = toScreen(c00.worldX, c00.worldY)
            val s10 = toScreen(c10.worldX, c10.worldY)
            val s11 = toScreen(c11.worldX, c11.worldY)
            val s01 = toScreen(c01.worldX, c01.worldY)

            val avgNorm = (c00.normalizedValue + c10.normalizedValue + c11.normalizedValue + c01.normalizedValue) / 4f
            val color = HeatmapGridCalculator.interpolateColor(palette, avgNorm)

            val path = Path().apply {
                moveTo(s00.x, s00.y)
                lineTo(s10.x, s10.y)
                lineTo(s11.x, s11.y)
                lineTo(s01.x, s01.y)
                close()
            }
            drawPath(path, color = color.copy(alpha = 0.7f))
        }
    }

    // Draw GPS path on top
    if (gridData.rawPoints.size > 1) {
        val pathOffsets = gridData.rawPoints.map { toScreen(it.relX, it.relY) }
        for (i in 0 until pathOffsets.size - 1) {
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = pathOffsets[i],
                end = pathOffsets[i + 1],
                strokeWidth = 2.5f
            )
        }
    }
}

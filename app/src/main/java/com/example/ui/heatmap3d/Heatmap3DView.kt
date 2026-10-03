package com.example.ui.heatmap3d

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SensorLogPoint
import com.example.sensor.ColorPalette
import com.example.sensor.HeatmapMetric
import com.example.sensor.Render3DMode
import com.example.sensor.SensorSnapshot
import kotlinx.coroutines.delay

@Composable
fun Heatmap3DView(
    snapshot: SensorSnapshot,
    gridData: HeatmapGridData,
    selectedMetric: HeatmapMetric,
    onMetricSelected: (HeatmapMetric) -> Unit,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    recordedPointsCount: Int,
    isDemoMode: Boolean,
    onToggleDemoMode: (Boolean) -> Unit,
    onResetOrigin: () -> Unit,
    activeSharedSessionTitle: String? = null,
    contributingDevicesCount: Int = 1,
    onOpenCloudRooms: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var cameraYaw by remember { mutableFloatStateOf(45f) }
    var cameraPitch by remember { mutableFloatStateOf(50f) }
    var cameraZoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var heightScale by remember { mutableFloatStateOf(1.3f) }

    var isAutoOrbit by remember { mutableStateOf(false) }
    var renderMode by remember { mutableStateOf(Render3DMode.TERRAIN_SURFACE) }
    var colorPalette by remember { mutableStateOf(ColorPalette.CYBER_NEON) }
    var showControlsPanel by remember { mutableStateOf(false) }
    var selectedPoint by remember { mutableStateOf<SensorLogPoint?>(null) }

    // Auto-orbit animation loop
    LaunchedEffect(isAutoOrbit) {
        while (isAutoOrbit) {
            delay(32)
            cameraYaw = (cameraYaw + 0.6f) % 360f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // 3D Canvas with Gesture detector
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        cameraZoom = (cameraZoom * zoom).coerceIn(0.4f, 4.0f)
                        // If panning with 2 fingers / large pan, adjust pan offset
                        panX += pan.x
                        panY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            // Reset camera
                            cameraYaw = 45f
                            cameraPitch = 50f
                            cameraZoom = 1.0f
                            panX = 0f
                            panY = 0f
                        },
                        onTap = { tapOffset ->
                            // Look for nearest point in rawPoints to inspect
                            val points = gridData.rawPoints
                            if (points.isNotEmpty()) {
                                selectedPoint = points.lastOrNull()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            if (changes.size == 1 && changes[0].pressed) {
                                val change = changes[0]
                                val dx = change.position.x - change.previousPosition.x
                                val dy = change.position.y - change.previousPosition.y
                                if (dx != 0f || dy != 0f) {
                                    cameraYaw = (cameraYaw + dx * 0.5f) % 360f
                                    cameraPitch = (cameraPitch - dy * 0.4f).coerceIn(15f, 85f)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            Heatmap3DCanvas(
                gridData = gridData,
                metric = selectedMetric,
                palette = colorPalette,
                renderMode = renderMode,
                cameraYaw = cameraYaw,
                cameraPitch = cameraPitch,
                cameraZoom = cameraZoom,
                panX = panX,
                panY = panY,
                heightScale = heightScale,
                selectedPoint = selectedPoint,
                onSelectPoint = { selectedPoint = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Status HUD & Metric Selector
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
        ) {
            // Live Telemetry Mini Bar
            Surface(
                color = Color(0xCC0D1B2A),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (snapshot.hasGpsFix) Color(0xFF00F5D4) else Color(0xFFFF9E00))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (snapshot.hasGpsFix) "GPS FIX" else "NO GPS FIX",
                            color = if (snapshot.hasGpsFix) Color(0xFF00F5D4) else Color(0xFFFF9E00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = String.format("%.0f Hz", snapshot.sampleRateHz),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${snapshot.activeSensorsCount} датч.",
                            color = Color(0xFF90E0EF),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${gridData.rawPoints.size} точек",
                            color = Color(0xFFFFD166),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (activeSharedSessionTitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xDD132A46),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCloudRooms() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = Color(0xFF00F5D4),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "СЕТЬ: $activeSharedSessionTitle",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "$contributingDevicesCount устр. онлайн",
                            color = Color(0xFF00F5D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metric Selection Horizontal Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeatmapMetric.values().forEach { metric ->
                    val isSelected = metric == selectedMetric
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMetricSelected(metric) },
                        label = {
                            Text(
                                text = metric.titleRu,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00F5D4),
                            selectedLabelColor = Color(0xFF090D16),
                            containerColor = Color(0x991B263B),
                            labelColor = Color(0xFFE0E1DD)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Color(0xFF00F5D4) else Color(0x44FFFFFF),
                            enabled = true,
                            selected = isSelected
                        ),
                        modifier = Modifier.testTag("metric_chip_${metric.name.lowercase()}")
                    )
                }
            }
        }

        // Floating Camera & Settings Tools (Right side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Toggle Auto-Orbit
            SmallFloatingActionButton(
                onClick = { isAutoOrbit = !isAutoOrbit },
                containerColor = if (isAutoOrbit) Color(0xFF00F5D4) else Color(0xCC1B263B),
                contentColor = if (isAutoOrbit) Color(0xFF090D16) else Color.White,
                modifier = Modifier.testTag("orbit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Авто-вращение 3D",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Mode Selector Toggle (Open panel)
            SmallFloatingActionButton(
                onClick = { showControlsPanel = !showControlsPanel },
                containerColor = if (showControlsPanel) Color(0xFF7209B7) else Color(0xCC1B263B),
                contentColor = Color.White,
                modifier = Modifier.testTag("layers_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Режимы 3D",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Reset Camera
            SmallFloatingActionButton(
                onClick = {
                    cameraYaw = 45f
                    cameraPitch = 50f
                    cameraZoom = 1.0f
                    panX = 0f
                    panY = 0f
                },
                containerColor = Color(0xCC1B263B),
                contentColor = Color.White,
                modifier = Modifier.testTag("reset_cam_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Сброс камеры",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Reset Origin
            SmallFloatingActionButton(
                onClick = onResetOrigin,
                containerColor = Color(0xCC1B263B),
                contentColor = Color(0xFF00F5D4),
                modifier = Modifier.testTag("reset_origin_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Центр в точку",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Extended 3D Controls Overlay Panel
        AnimatedVisibility(
            visible = showControlsPanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0B132B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400F5D4)),
                modifier = Modifier.width(230.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "НАСТРОЙКИ 3D",
                        color = Color(0xFF00F5D4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "Стиль визуализации:",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Render3DMode.values().forEach { mode ->
                        val isSel = mode == renderMode
                        Surface(
                            color = if (isSel) Color(0x4400F5D4) else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { renderMode = mode }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = mode.titleRu,
                                color = if (isSel) Color(0xFF00F5D4) else Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Цветовая шкала:",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ColorPalette.values().forEach { pal ->
                            val isSel = pal == colorPalette
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(pal.colors)
                                    )
                                    .border(
                                        width = if (isSel) 2.dp else 0.dp,
                                        color = if (isSel) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { colorPalette = pal }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("Высота 3D: %.1fx", heightScale),
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = heightScale,
                        onValueChange = { heightScale = it },
                        valueRange = 0.4f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F5D4),
                            activeTrackColor = Color(0xFF00F5D4)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }

        // Bottom Bar: Color Legend + Record Controller & Live Value
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selected point inspection card if active
            selectedPoint?.let { pt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD0D1B2A)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7209B7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ТОЧКА №${pt.id} • Смещение: (${String.format("%.1f", pt.relX)}м, ${String.format("%.1f", pt.relY)}м)",
                                color = Color(0xFF00F5D4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "EMF: ${String.format("%.1f", pt.magMagnitude)}µT | Вибрация: ${String.format("%.1f", pt.accelMagnitude)}м/с² | Свет: ${pt.lightLux.toInt()}Lx | Давл.: ${String.format("%.1f", pt.pressureHpa)}hPa",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { selectedPoint = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("✕", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Legend Gradient Strip with Min/Max
            Surface(
                color = Color(0xBB0D1B2A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.horizontalGradient(colorPalette.colors))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MIN: ${selectedMetric.formatValue(gridData.minValue)}",
                            color = Color(0xFF90E0EF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "ИНТЕНСИВНОСТЬ: ${selectedMetric.titleRu}",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "MAX: ${selectedMetric.formatValue(gridData.maxValue)}",
                            color = Color(0xFFFFD166),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Main Recording and Live Metrics Panel
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0B132B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current Live Value Display
                    Column {
                        Text(
                            text = "ТЕКУЩИЙ ПОКАЗАТЕЛЬ",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = selectedMetric.formatValue(snapshot.getMetricValue(selectedMetric)),
                            color = Color(0xFF00F5D4),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Demo Walk Mode Toggle
                    Button(
                        onClick = { onToggleDemoMode(!isDemoMode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDemoMode) Color(0xFF7209B7) else Color(0x337209B7)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("demo_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDemoMode) "Демо ВКЛ" else "Демо 3D",
                            fontSize = 12.sp
                        )
                    }

                    // Recording Action Button
                    Button(
                        onClick = {
                            if (isRecording) onStopRecording() else onStartRecording()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFEF476F) else Color(0xFF06D6A0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("record_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Остановить запись" else "Начать запись",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) "СТОП ($recordedPointsCount)" else "ЗАПИСЬ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

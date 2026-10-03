package com.example.ui.telemetry

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.HardwareSensorInfo
import com.example.sensor.SamplingRate
import com.example.sensor.SensorSnapshot

@Composable
fun TelemetryDashboardScreen(
    snapshot: SensorSnapshot,
    hardwareSensors: List<HardwareSensorInfo>,
    accelHistoryX: List<Float>,
    accelHistoryY: List<Float>,
    accelHistoryZ: List<Float>,
    magHistoryX: List<Float>,
    magHistoryY: List<Float>,
    magHistoryZ: List<Float>,
    gyroHistoryX: List<Float>,
    gyroHistoryY: List<Float>,
    gyroHistoryZ: List<Float>,
    currentSamplingRate: SamplingRate,
    onSelectSamplingRate: (SamplingRate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Sampling Frequency & GPS Status Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Color(0xFF00F5D4),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ТЕЛЕМЕТРИЯ ДАТЧИКОВ",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            color = Color(0x3300F5D4),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = String.format("%.0f HZ ПОТОК", snapshot.sampleRateHz),
                                color = Color(0xFF00F5D4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Частота дискретизации сенсорного конвейера:",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SamplingRate.values().forEach { rate ->
                            val isSel = rate == currentSamplingRate
                            FilterChip(
                                selected = isSel,
                                onClick = { onSelectSamplingRate(rate) },
                                label = {
                                    Text(
                                        text = "${rate.titleRu} (${rate.approximateHz})",
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00F5D4),
                                    selectedLabelColor = Color(0xFF090D16),
                                    containerColor = Color(0x331B263B),
                                    labelColor = Color(0xFFE0E1DD)
                                ),
                                modifier = Modifier.testTag("rate_${rate.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // GPS Telemetry Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = if (snapshot.hasGpsFix) Color(0xFF00F5D4) else Color(0xFFFF9E00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ВЫСОКОТОЧНЫЙ GPS & ПОЗИЦИЯ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = if (snapshot.hasGpsFix) "Точность: ±${String.format("%.1f", snapshot.gpsAccuracyMeters)}м" else "Ожидание фикса",
                            color = if (snapshot.hasGpsFix) Color(0xFF00F5D4) else Color(0xFFFF9E00),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Широта (Lat):", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.6f°", snapshot.latitude), color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("Долгота (Lon):", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.6f°", snapshot.longitude), color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("Высота GPS:", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.1f м", snapshot.altitudeMeters), color = Color(0xFF00F5D4), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("Скорость:", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.1f км/ч", snapshot.speedKmh), color = Color(0xFFFFD166), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Live Oscilloscope: Accelerometer
        item {
            SensorGraphCard(
                title = "АКСЕЛЕРОМЕТР (Линейное ускорение)",
                totalMagnitude = String.format("%.2f м/с²", snapshot.accelMag),
                valX = String.format("X: %.2f", snapshot.accelX),
                valY = String.format("Y: %.2f", snapshot.accelY),
                valZ = String.format("Z: %.2f", snapshot.accelZ),
                xHistory = accelHistoryX,
                yHistory = accelHistoryY,
                zHistory = accelHistoryZ,
                minVal = -15f,
                maxVal = 25f
            )
        }

        // Live Oscilloscope: Magnetometer
        item {
            SensorGraphCard(
                title = "МАГНИТОМЕТР (Индукция магнитного поля)",
                totalMagnitude = String.format("%.1f µT", snapshot.magMag),
                valX = String.format("X: %.1f", snapshot.magX),
                valY = String.format("Y: %.1f", snapshot.magY),
                valZ = String.format("Z: %.1f", snapshot.magZ),
                xHistory = magHistoryX,
                yHistory = magHistoryY,
                zHistory = magHistoryZ,
                minVal = -100f,
                maxVal = 100f
            )
        }

        // Live Oscilloscope: Gyroscope
        item {
            SensorGraphCard(
                title = "ГИРОСКОП (Угловая скорость)",
                totalMagnitude = String.format("%.2f рад/с", snapshot.gyroMag),
                valX = String.format("X: %.2f", snapshot.gyroX),
                valY = String.format("Y: %.2f", snapshot.gyroY),
                valZ = String.format("Z: %.2f", snapshot.gyroZ),
                xHistory = gyroHistoryX,
                yHistory = gyroHistoryY,
                zHistory = gyroHistoryZ,
                minVal = -5f,
                maxVal = 5f
            )
        }

        // Environmental Sensors Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Light Lux Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2200F5D4)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ОСВЕЩЕННОСТЬ", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${snapshot.lightLux.toInt()} Lux",
                            color = Color(0xFFFFD166),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("Датчик света", color = Color.DarkGray, fontSize = 10.sp)
                    }
                }

                // Pressure Barometer Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2200F5D4)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ДАВЛЕНИЕ", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.1f hPa", snapshot.pressureHpa),
                            color = Color(0xFF00F5D4),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("Барометр", color = Color.DarkGray, fontSize = 10.sp)
                    }
                }
            }
        }

        // Spatial Orientation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = null,
                                tint = Color(0xFFEF476F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ОРИЕНТАЦИЯ В ПРОСТРАНСТВЕ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Азимут (Heading):", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.1f°", snapshot.azimuthDeg), color = Color(0xFFEF476F), fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Тангаж (Pitch):", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.1f°", snapshot.pitchDeg), color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("Крен (Roll):", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.1f°", snapshot.rollDeg), color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("Шаги:", color = Color.Gray, fontSize = 11.sp)
                            Text("${snapshot.stepCount}", color = Color(0xFF00F5D4), fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Hardware Sensor Inventory Section Header
        item {
            Text(
                text = "АППАРАТНЫЙ ИНВЕНТАРЬ ДАТЧИКОВ УСТРОЙСТВА",
                color = Color(0xFF00F5D4),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(hardwareSensors) { sensor ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (sensor.isAvailable) Color(0x2200F5D4) else Color(0x22FF0055)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (sensor.isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (sensor.isAvailable) Color(0xFF06D6A0) else Color(0xFFEF476F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sensor.typeNameRu,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${sensor.name} • ${sensor.vendor}",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        if (sensor.isAvailable) {
                            Text(
                                text = "Диапазон: ${sensor.maxRange} | Точность: ${sensor.resolution} | Ток: ${sensor.powerMa} mA",
                                color = Color(0xFF90E0EF),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        color = if (sensor.isAvailable) Color(0x3306D6A0) else Color(0x33EF476F),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (sensor.isAvailable) "АКТИВЕН" else "НЕТ ДАТЧИКА",
                            color = if (sensor.isAvailable) Color(0xFF06D6A0) else Color(0xFFEF476F),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorGraphCard(
    title: String,
    totalMagnitude: String,
    valX: String,
    valY: String,
    valZ: String,
    xHistory: List<Float>,
    yHistory: List<Float>,
    zHistory: List<Float>,
    minVal: Float,
    maxVal: Float
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5D4)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = totalMagnitude,
                    color = Color(0xFF00F5D4),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Axes legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(valX, color = Color(0xFFFF0055), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(valY, color = Color(0xFF00F5D4), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(valZ, color = Color(0xFFFFD166), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OscilloscopeChart(
                xHistory = xHistory,
                yHistory = yHistory,
                zHistory = zHistory,
                minVal = minVal,
                maxVal = maxVal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
            )
        }
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_points")
data class SensorLogPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    
    // Spatial & GPS
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val speedKmh: Float,
    val bearing: Float,
    val relX: Float, // relative meters X
    val relY: Float, // relative meters Y
    
    // Sensor readings
    val accelMagnitude: Float,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroMagnitude: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val magMagnitude: Float,
    val magX: Float,
    val magY: Float,
    val magZ: Float,
    val lightLux: Float,
    val pressureHpa: Float,
    val proximityCm: Float,
    val pitchDeg: Float,
    val rollDeg: Float,
    val azimuthDeg: Float,
    val stepCount: Int
)

@Entity(tableName = "sensor_sessions")
data class SensorSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val pointCount: Int = 0,
    val primaryMetric: String,
    val maxMetricValue: Float = 0f,
    val avgMetricValue: Float = 0f,
    val distanceMeters: Float = 0f
)

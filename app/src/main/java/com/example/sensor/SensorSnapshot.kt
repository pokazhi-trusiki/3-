package com.example.sensor

data class SensorSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    
    // Accelerometer (m/s^2)
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.8f,
    val accelMag: Float = 9.8f,
    val linearAccelMag: Float = 0f,
    
    // Gyroscope (rad/s)
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val gyroMag: Float = 0f,
    
    // Magnetometer (microTesla uT)
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val magMag: Float = 45f,
    
    // Environmental
    val lightLux: Float = 250f,
    val pressureHpa: Float = 1013.25f,
    val proximityCm: Float = 5f,
    val ambientTempC: Float = 22f,
    val relativeHumidity: Float = 45f,
    
    // Orientation angles (degrees)
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val azimuthDeg: Float = 0f,
    
    // Step counter
    val stepCount: Int = 0,
    
    // GPS Location
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val speedKmh: Float = 0f,
    val gpsAccuracyMeters: Float = 0f,
    val hasGpsFix: Boolean = false,
    
    // Derived local relative coords (meters from session origin)
    val relX: Float = 0f,
    val relY: Float = 0f,
    
    // Polling health
    val sampleRateHz: Float = 0f,
    val activeSensorsCount: Int = 0
) {
    fun getMetricValue(metric: HeatmapMetric): Float {
        return when (metric) {
            HeatmapMetric.MAGNETIC_FIELD -> magMag
            HeatmapMetric.ACCELEROMETER -> accelMag
            HeatmapMetric.GYROSCOPE -> gyroMag
            HeatmapMetric.AMBIENT_LIGHT -> lightLux
            HeatmapMetric.BAROMETRIC_PRESSURE -> pressureHpa
            HeatmapMetric.GPS_SPEED -> speedKmh
            HeatmapMetric.COMPOSITE_STRESS -> {
                val accelNorm = (accelMag - 9.8f).coerceAtLeast(0f) * 4f
                val magNorm = (magMag - 45f).coerceAtLeast(0f) * 0.5f
                val gyroNorm = gyroMag * 10f
                (accelNorm + magNorm + gyroNorm).coerceIn(0f, 100f)
            }
        }
    }
}

data class HardwareSensorInfo(
    val name: String,
    val type: Int,
    val typeNameRu: String,
    val vendor: String,
    val maxRange: Float,
    val resolution: Float,
    val powerMa: Float,
    val isAvailable: Boolean
)

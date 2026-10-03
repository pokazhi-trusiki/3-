package com.example.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class SamplingRate(val delayUs: Int, val titleRu: String, val approximateHz: String) {
    FASTEST(SensorManager.SENSOR_DELAY_FASTEST, "Максимум", "~100-200 Hz"),
    GAME(SensorManager.SENSOR_DELAY_GAME, "Игровой", "~50 Hz"),
    UI(SensorManager.SENSOR_DELAY_UI, "Интерфейс", "~15 Hz"),
    NORMAL(SensorManager.SENSOR_DELAY_NORMAL, "Стандарт", "~5 Hz")
}

class SensorTrackingManager(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _snapshot = MutableStateFlow(SensorSnapshot())
    val snapshot: StateFlow<SensorSnapshot> = _snapshot.asStateFlow()

    private val _hardwareSensors = MutableStateFlow<List<HardwareSensorInfo>>(emptyList())
    val hardwareSensors: StateFlow<List<HardwareSensorInfo>> = _hardwareSensors.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private var currentSamplingRate: SamplingRate = SamplingRate.GAME

    // Origin reference for relative coordinates (meters)
    private var originLatitude: Double? = null
    private var originLongitude: Double? = null

    // PDR (Pedestrian Dead Reckoning) fallback accumulator
    private var pdrX: Float = 0f
    private var pdrY: Float = 0f
    private var lastStepTime: Long = 0L

    // Sampling frequency calculation
    private var sampleCounter = 0
    private var lastSampleCalcTime = System.currentTimeMillis()
    private var currentCalculatedHz = 0f

    // Live intermediate values
    private var accelX = 0f
    private var accelY = 0f
    private var accelZ = 9.8f
    private var linearAccelMag = 0f
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f
    private var magX = 0f
    private var magY = 0f
    private var magZ = 0f
    private var lightLux = 200f
    private var pressureHpa = 1013.25f
    private var proximityCm = 5f
    private var ambientTempC = 22f
    private var relativeHumidity = 45f
    private var pitchDeg = 0f
    private var rollDeg = 0f
    private var azimuthDeg = 0f
    private var stepCount = 0

    private var currentLat = 55.7558
    private var currentLon = 37.6173
    private var currentAlt = 150.0
    private var currentSpeedKmh = 0f
    private var currentAccuracyM = 10f
    private var hasGpsFix = false

    private var locationCallback: LocationCallback? = null
    private var demoJob: Job? = null
    private var hzJob: Job? = null

    init {
        inspectHardwareSensors()
        startHzMonitoring()
    }

    private fun inspectHardwareSensors() {
        val sm = sensorManager ?: return
        val sensorTypes = listOf(
            Triple(Sensor.TYPE_ACCELEROMETER, "Акселерометр", "Измерение линейного ускорения и вибраций"),
            Triple(Sensor.TYPE_GYROSCOPE, "Гироскоп", "Угловая скорость вращения по трем осям"),
            Triple(Sensor.TYPE_MAGNETIC_FIELD, "Магнитометр", "Индукция магнитного поля Земли и электромагнитных источников"),
            Triple(Sensor.TYPE_LIGHT, "Датчик освещенности", "Уровень освещенности внешней среды в люксах"),
            Triple(Sensor.TYPE_PRESSURE, "Барометр", "Атмосферное давление для сверхточного вычисления высоты"),
            Triple(Sensor.TYPE_PROXIMITY, "Датчик приближения", "Определение расстояния до препятствия"),
            Triple(Sensor.TYPE_LINEAR_ACCELERATION, "Линейное ускорение", "Динамическое ускорение без учета гравитации"),
            Triple(Sensor.TYPE_ROTATION_VECTOR, "Вектор вращения", "Пространственная ориентация устройства (кватернион)"),
            Triple(Sensor.TYPE_STEP_COUNTER, "Шагомер", "Аппаратный счетчик шагов пользователя"),
            Triple(Sensor.TYPE_AMBIENT_TEMPERATURE, "Термометр", "Температура окружающей среды"),
            Triple(Sensor.TYPE_RELATIVE_HUMIDITY, "Гигрометр", "Относительная влажность воздуха")
        )

        val list = sensorTypes.map { (type, nameRu, _) ->
            val sensor = sm.getDefaultSensor(type)
            HardwareSensorInfo(
                name = sensor?.name ?: nameRu,
                type = type,
                typeNameRu = nameRu,
                vendor = sensor?.vendor ?: "Н/Д",
                maxRange = sensor?.maximumRange ?: 0f,
                resolution = sensor?.resolution ?: 0f,
                powerMa = sensor?.power ?: 0f,
                isAvailable = sensor != null
            )
        }
        _hardwareSensors.value = list
    }

    fun startTracking(samplingRate: SamplingRate = currentSamplingRate) {
        currentSamplingRate = samplingRate
        val sm = sensorManager ?: return

        // Register all sensor listeners
        val sensorsToRegister = listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY
        )

        sensorsToRegister.forEach { type ->
            sm.getDefaultSensor(type)?.let { sensor ->
                sm.registerListener(this, sensor, samplingRate.delayUs)
            }
        }

        startGpsTracking()
        _isTracking.value = true
    }

    fun stopTracking() {
        sensorManager?.unregisterListener(this)
        stopGpsTracking()
        stopDemoMode()
        _isTracking.value = false
    }

    fun setSamplingRate(samplingRate: SamplingRate) {
        if (currentSamplingRate != samplingRate) {
            currentSamplingRate = samplingRate
            if (_isTracking.value) {
                sensorManager?.unregisterListener(this)
                startTracking(samplingRate)
            }
        }
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        if (enabled) {
            startDemoSimulation()
        } else {
            stopDemoMode()
        }
    }

    fun resetSpatialOrigin() {
        originLatitude = currentLat
        originLongitude = currentLon
        pdrX = 0f
        pdrY = 0f
        updateSnapshot()
    }

    @SuppressLint("MissingPermission")
    private fun startGpsTracking() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setMinUpdateDistanceMeters(0.2f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    onNewLocation(loc)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    onNewLocation(loc)
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopGpsTracking() {
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (_: Exception) {}
            locationCallback = null
        }
    }

    private fun onNewLocation(loc: Location) {
        currentLat = loc.latitude
        currentLon = loc.longitude
        currentAlt = loc.altitude
        currentSpeedKmh = loc.speed * 3.6f
        currentAccuracyM = loc.accuracy
        hasGpsFix = true

        if (originLatitude == null || originLongitude == null) {
            originLatitude = currentLat
            originLongitude = currentLon
        }

        updateSnapshot()
    }

    override fun onSensorChanged(event: SensorEvent) {
        sampleCounter++
        val values = event.values

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelX = values[0]
                accelY = values[1]
                accelZ = values[2]
                detectDeadReckoningStep()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                linearAccelMag = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroX = values[0]
                gyroY = values[1]
                gyroZ = values[2]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magX = values[0]
                magY = values[1]
                magZ = values[2]
            }
            Sensor.TYPE_LIGHT -> {
                lightLux = values[0]
            }
            Sensor.TYPE_PRESSURE -> {
                pressureHpa = values[0]
            }
            Sensor.TYPE_PROXIMITY -> {
                proximityCm = values[0]
            }
            Sensor.TYPE_STEP_COUNTER -> {
                stepCount = values[0].toInt()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
                val orientationAngles = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                // Orientation in degrees
                azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                ambientTempC = values[0]
            }
            Sensor.TYPE_RELATIVE_HUMIDITY -> {
                relativeHumidity = values[0]
            }
        }

        updateSnapshot()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun detectDeadReckoningStep() {
        val totalAccel = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
        val now = System.currentTimeMillis()
        // If device experiences a step impulse and at least 300ms passed
        if (totalAccel > 12.0f && (now - lastStepTime) > 320) {
            lastStepTime = now
            val stepLengthM = 0.7f
            val rad = Math.toRadians(azimuthDeg.toDouble())
            pdrX += (stepLengthM * sin(rad)).toFloat()
            pdrY += (stepLengthM * cos(rad)).toFloat()
        }
    }

    private fun calculateRelativeCoords(): Pair<Float, Float> {
        val oLat = originLatitude
        val oLon = originLongitude
        if (oLat == null || oLon == null || !hasGpsFix) {
            return Pair(pdrX, pdrY)
        }

        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(currentLat - oLat)
        val dLon = Math.toRadians(currentLon - oLon)
        val meanLat = Math.toRadians((currentLat + oLat) / 2.0)

        val x = (dLon * cos(meanLat) * earthRadius).toFloat()
        val y = (dLat * earthRadius).toFloat()

        // If GPS distance is very small (e.g. indoors/desk), combine with PDR
        val gpsDist = sqrt(x * x + y * y)
        return if (gpsDist < 1.0f && (pdrX != 0f || pdrY != 0f)) {
            Pair(pdrX, pdrY)
        } else {
            Pair(x, y)
        }
    }

    private fun updateSnapshot() {
        val accelMag = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
        val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
        val magMag = sqrt(magX * magX + magY * magY + magZ * magZ)
        val (rx, ry) = calculateRelativeCoords()

        _snapshot.value = SensorSnapshot(
            timestamp = System.currentTimeMillis(),
            accelX = accelX,
            accelY = accelY,
            accelZ = accelZ,
            accelMag = accelMag,
            linearAccelMag = linearAccelMag,
            gyroX = gyroX,
            gyroY = gyroY,
            gyroZ = gyroZ,
            gyroMag = gyroMag,
            magX = magX,
            magY = magY,
            magZ = magZ,
            magMag = magMag,
            lightLux = lightLux,
            pressureHpa = pressureHpa,
            proximityCm = proximityCm,
            ambientTempC = ambientTempC,
            relativeHumidity = relativeHumidity,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            azimuthDeg = azimuthDeg,
            stepCount = stepCount,
            latitude = currentLat,
            longitude = currentLon,
            altitudeMeters = currentAlt,
            speedKmh = currentSpeedKmh,
            gpsAccuracyMeters = currentAccuracyM,
            hasGpsFix = hasGpsFix,
            relX = rx,
            relY = ry,
            sampleRateHz = currentCalculatedHz,
            activeSensorsCount = _hardwareSensors.value.count { it.isAvailable }
        )
    }

    private fun startHzMonitoring() {
        hzJob?.cancel()
        hzJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val deltaSec = (now - lastSampleCalcTime) / 1000.0
                if (deltaSec > 0) {
                    currentCalculatedHz = (sampleCounter / deltaSec).toFloat()
                }
                sampleCounter = 0
                lastSampleCalcTime = now
            }
        }
    }

    private fun startDemoSimulation() {
        demoJob?.cancel()
        demoJob = scope.launch(Dispatchers.Default) {
            var t = 0.0
            hasGpsFix = true
            if (originLatitude == null) {
                originLatitude = 55.7558
                originLongitude = 37.6173
            }

            while (isActive) {
                t += 0.08
                // Simulate an exploratory trajectory (Lissajous/spiral walking path)
                val radius = 25.0 * (1.0 + 0.3 * sin(t * 0.2))
                val simX = (radius * sin(t)).toFloat()
                val simY = (radius * cos(t * 0.7)).toFloat()

                // Simulated dynamic sensor readings with realistic localized hotspots
                // Hotspot 1: EMF peak at (+12, -8)
                val distHotspot1 = sqrt((simX - 12.0) * (simX - 12.0) + (simY + 8.0) * (simY + 8.0))
                val emfAnomaly = (95.0 / (1.0 + distHotspot1 * 0.3)).toFloat()

                // Hotspot 2: Vibration bump at (-10, +15)
                val distHotspot2 = sqrt((simX + 10.0) * (simX + 10.0) + (simY - 15.0) * (simY - 15.0))
                val vibAnomaly = (18.0 / (1.0 + distHotspot2 * 0.4)).toFloat()

                accelX = (sin(t * 4.0) * 1.5).toFloat()
                accelY = (cos(t * 4.0) * 1.2).toFloat()
                accelZ = (9.8f + (sin(t * 2.0) * 0.8).toFloat() + vibAnomaly)

                gyroX = (cos(t * 1.5) * 0.4).toFloat()
                gyroY = (sin(t * 1.2) * 0.5).toFloat()
                gyroZ = (sin(t * 0.8) * 0.9).toFloat()

                magX = (28f + emfAnomaly * 0.7f + (sin(t * 0.5) * 4f).toFloat())
                magY = (-15f + emfAnomaly * 0.5f + (cos(t * 0.6) * 3f).toFloat())
                magZ = (40f + emfAnomaly * 0.9f)

                lightLux = (350f + (sin(t * 0.3) * 280f).toFloat()).coerceAtLeast(10f)
                pressureHpa = (1012.5f + (sin(t * 0.1) * 3.5f).toFloat())

                azimuthDeg = ((t * 25.0) % 360.0).toFloat()
                pitchDeg = (sin(t * 0.8) * 15.0).toFloat()
                rollDeg = (cos(t * 0.6) * 10.0).toFloat()

                pdrX = simX
                pdrY = simY

                currentLat = (originLatitude ?: 55.7558) + (simY / 111000.0)
                currentLon = (originLongitude ?: 37.6173) + (simX / (111000.0 * cos(Math.toRadians(55.7558))))
                currentAlt = 150.0 + (sin(t * 0.2) * 8.0)
                currentSpeedKmh = 4.8f + (sin(t) * 1.2f).toFloat()
                currentAccuracyM = 3.5f
                sampleCounter += 3

                updateSnapshot()
                delay(50) // ~20 Hz demo updates
            }
        }
    }

    private fun stopDemoMode() {
        demoJob?.cancel()
        demoJob = null
    }

    fun cleanUp() {
        stopTracking()
        hzJob?.cancel()
    }
}

package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SensorLogPoint
import com.example.data.SensorRepository
import com.example.data.SensorSession
import com.example.firestore.SharedHeatmapRepository
import com.example.firestore.SharedSessionDoc
import com.example.firestore.SharedTelemetryPointDoc
import com.example.sensor.HardwareSensorInfo
import com.example.sensor.HeatmapMetric
import com.example.sensor.SamplingRate
import com.example.sensor.SensorSnapshot
import com.example.sensor.SensorTrackingManager
import com.example.ui.ai.SensorAiReport
import com.example.ui.ai.SensorAiService
import com.example.ui.heatmap3d.HeatmapGridCalculator
import com.example.ui.heatmap3d.HeatmapGridData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class AppNavTab(val titleRu: String) {
    HEATMAP_3D("3D Карта"),
    TELEMETRY("Датчики"),
    CLOUD_MESH("Мульти-сеть"),
    SESSIONS("Сессии"),
    AI_DIAGNOSTICS("AI Анализ")
}

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = SensorRepository(database.sensorDao())
    val sharedRepository = SharedHeatmapRepository(application)

    val trackingManager = SensorTrackingManager(application, viewModelScope)

    val snapshot: StateFlow<SensorSnapshot> = trackingManager.snapshot
    val hardwareSensors: StateFlow<List<HardwareSensorInfo>> = trackingManager.hardwareSensors
    val isTracking: StateFlow<Boolean> = trackingManager.isTracking
    val isDemoMode: StateFlow<Boolean> = trackingManager.isDemoMode

    private val _currentTab = MutableStateFlow(AppNavTab.HEATMAP_3D)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    private val _selectedMetric = MutableStateFlow(HeatmapMetric.MAGNETIC_FIELD)
    val selectedMetric: StateFlow<HeatmapMetric> = _selectedMetric.asStateFlow()

    private val _currentSamplingRate = MutableStateFlow(SamplingRate.GAME)
    val currentSamplingRate: StateFlow<SamplingRate> = _currentSamplingRate.asStateFlow()

    // 3D Heatmap Display Points & Grid
    private val _livePoints = MutableStateFlow<List<SensorLogPoint>>(emptyList())
    private val _gridData = MutableStateFlow(
        HeatmapGridCalculator.computeGrid(emptyList(), HeatmapMetric.MAGNETIC_FIELD)
    )
    val gridData: StateFlow<HeatmapGridData> = _gridData.asStateFlow()

    // Multi-Device Cloud Rooms & Sync
    val sharedSessions: StateFlow<List<SharedSessionDoc>> = sharedRepository.observeSharedSessions()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeSharedSession = MutableStateFlow<SharedSessionDoc?>(null)
    val activeSharedSession: StateFlow<SharedSessionDoc?> = _activeSharedSession.asStateFlow()

    private val _sharedPoints = MutableStateFlow<List<SharedTelemetryPointDoc>>(emptyList())
    val sharedPoints: StateFlow<List<SharedTelemetryPointDoc>> = _sharedPoints.asStateFlow()

    private val _contributingDevicesCount = MutableStateFlow(1)
    val contributingDevicesCount: StateFlow<Int> = _contributingDevicesCount.asStateFlow()

    private var sharedPointsSubscriptionJob: Job? = null

    // Oscilloscope Rolling Buffers
    private val _accelHistoryX = MutableStateFlow<List<Float>>(emptyList())
    val accelHistoryX: StateFlow<List<Float>> = _accelHistoryX.asStateFlow()
    private val _accelHistoryY = MutableStateFlow<List<Float>>(emptyList())
    val accelHistoryY: StateFlow<List<Float>> = _accelHistoryY.asStateFlow()
    private val _accelHistoryZ = MutableStateFlow<List<Float>>(emptyList())
    val accelHistoryZ: StateFlow<List<Float>> = _accelHistoryZ.asStateFlow()

    private val _magHistoryX = MutableStateFlow<List<Float>>(emptyList())
    val magHistoryX: StateFlow<List<Float>> = _magHistoryX.asStateFlow()
    private val _magHistoryY = MutableStateFlow<List<Float>>(emptyList())
    val magHistoryY: StateFlow<List<Float>> = _magHistoryY.asStateFlow()
    private val _magHistoryZ = MutableStateFlow<List<Float>>(emptyList())
    val magHistoryZ: StateFlow<List<Float>> = _magHistoryZ.asStateFlow()

    private val _gyroHistoryX = MutableStateFlow<List<Float>>(emptyList())
    val gyroHistoryX: StateFlow<List<Float>> = _gyroHistoryX.asStateFlow()
    private val _gyroHistoryY = MutableStateFlow<List<Float>>(emptyList())
    val gyroHistoryY: StateFlow<List<Float>> = _gyroHistoryY.asStateFlow()
    private val _gyroHistoryZ = MutableStateFlow<List<Float>>(emptyList())
    val gyroHistoryZ: StateFlow<List<Float>> = _gyroHistoryZ.asStateFlow()

    // Recording State (Local & Cloud)
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private var activeLocalSessionId: Long? = null
    private val uncommittedPoints = ArrayList<SensorLogPoint>()
    private var distanceAccumulator = 0f
    private var lastRecordedX = 0f
    private var lastRecordedY = 0f
    private var maxRecordedMetric = 0f
    private var sumRecordedMetric = 0.0
    private var recordedCount = 0
    private var lastCloudBroadcastTime = 0L

    private val _activeViewingSessionId = MutableStateFlow<Long?>(null)
    val activeViewingSessionId: StateFlow<Long?> = _activeViewingSessionId.asStateFlow()

    // Local Sessions from DB
    val sessions: StateFlow<List<SensorSession>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // AI Analysis State
    private val _aiReport = MutableStateFlow<SensorAiReport?>(null)
    val aiReport: StateFlow<SensorAiReport?> = _aiReport.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private var samplingCollectorJob: Job? = null
    private var lastGridComputeTime = 0L

    init {
        // Start live tracking immediately
        trackingManager.startTracking(_currentSamplingRate.value)
        startObservingSnapshot()

        // Sync user profile if already authenticated
        FirebaseAuth.getInstance().currentUser?.let { user ->
            viewModelScope.launch(Dispatchers.IO) {
                sharedRepository.syncUserProfile(user)
            }
        }
    }

    private fun startObservingSnapshot() {
        samplingCollectorJob = viewModelScope.launch(Dispatchers.Default) {
            snapshot.collect { snap ->
                // Update oscilloscope buffers (limit to 40 samples)
                updateOscilloscopeBuffers(snap)

                val now = System.currentTimeMillis()

                // If in active shared session and tracking, stream to cloud at controlled rate (~5 Hz)
                val currentShared = _activeSharedSession.value
                if (currentShared != null && (now - lastCloudBroadcastTime) > 200) {
                    lastCloudBroadcastTime = now
                    viewModelScope.launch(Dispatchers.IO) {
                        sharedRepository.contributePoint(
                            sessionId = currentShared.id,
                            latitude = snap.latitude,
                            longitude = snap.longitude,
                            altitude = snap.altitudeMeters,
                            accuracy = snap.gpsAccuracyMeters,
                            speedKmh = snap.speedKmh,
                            bearing = snap.azimuthDeg,
                            relX = snap.relX,
                            relY = snap.relY,
                            accelMag = snap.accelMag,
                            magMag = snap.magMag,
                            gyroMag = snap.gyroMag,
                            lightLux = snap.lightLux,
                            pressureHpa = snap.pressureHpa
                        )
                    }
                }

                // If recording locally, save to Room
                if (_isRecording.value) {
                    recordPointFromSnapshot(snap, now)
                } else if (_activeSharedSession.value == null && _livePoints.value.isEmpty()) {
                    addAmbientLivePoint(snap)
                }

                // Throttle 3D Grid re-computation for smooth UI performance (~10 Hz)
                if (now - lastGridComputeTime > 120 && _livePoints.value.isNotEmpty()) {
                    lastGridComputeTime = now
                    recomputeGrid()
                }
            }
        }
    }

    private fun updateOscilloscopeBuffers(snap: SensorSnapshot) {
        val maxSamples = 40
        _accelHistoryX.value = (_accelHistoryX.value + snap.accelX).takeLast(maxSamples)
        _accelHistoryY.value = (_accelHistoryY.value + snap.accelY).takeLast(maxSamples)
        _accelHistoryZ.value = (_accelHistoryZ.value + snap.accelZ).takeLast(maxSamples)

        _magHistoryX.value = (_magHistoryX.value + snap.magX).takeLast(maxSamples)
        _magHistoryY.value = (_magHistoryY.value + snap.magY).takeLast(maxSamples)
        _magHistoryZ.value = (_magHistoryZ.value + snap.magZ).takeLast(maxSamples)

        _gyroHistoryX.value = (_gyroHistoryX.value + snap.gyroX).takeLast(maxSamples)
        _gyroHistoryY.value = (_gyroHistoryY.value + snap.gyroY).takeLast(maxSamples)
        _gyroHistoryZ.value = (_gyroHistoryZ.value + snap.gyroZ).takeLast(maxSamples)
    }

    private fun addAmbientLivePoint(snap: SensorSnapshot) {
        val newPoint = createPointFromSnapshot(snap, 0L)
        val currentList = _livePoints.value
        if (currentList.isEmpty() || Math.abs(currentList.last().relX - snap.relX) > 0.15f || Math.abs(currentList.last().relY - snap.relY) > 0.15f) {
            _livePoints.value = (currentList + newPoint).takeLast(250)
        }
    }

    private fun recordPointFromSnapshot(snap: SensorSnapshot, timestamp: Long) {
        val sid = activeLocalSessionId ?: return
        val pt = createPointFromSnapshot(snap, sid)

        if (recordedCount > 0) {
            val dx = pt.relX - lastRecordedX
            val dy = pt.relY - lastRecordedY
            distanceAccumulator += sqrt(dx * dx + dy * dy)
        }
        lastRecordedX = pt.relX
        lastRecordedY = pt.relY

        val metricVal = pt.extractMetric(_selectedMetric.value)
        if (metricVal > maxRecordedMetric) maxRecordedMetric = metricVal
        sumRecordedMetric += metricVal
        recordedCount++

        uncommittedPoints.add(pt)
        if (_activeSharedSession.value == null) {
            _livePoints.value = _livePoints.value + pt
        }

        // Batch save to Room database every 20 points
        if (uncommittedPoints.size >= 20) {
            val batch = ArrayList(uncommittedPoints)
            uncommittedPoints.clear()
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveBatchPoints(batch)
            }
        }
    }

    private fun createPointFromSnapshot(snap: SensorSnapshot, sid: Long): SensorLogPoint {
        return SensorLogPoint(
            sessionId = sid,
            timestamp = snap.timestamp,
            latitude = snap.latitude,
            longitude = snap.longitude,
            altitude = snap.altitudeMeters,
            accuracy = snap.gpsAccuracyMeters,
            speedKmh = snap.speedKmh,
            bearing = snap.azimuthDeg,
            relX = snap.relX,
            relY = snap.relY,
            accelMagnitude = snap.accelMag,
            accelX = snap.accelX,
            accelY = snap.accelY,
            accelZ = snap.accelZ,
            gyroMagnitude = snap.gyroMag,
            gyroX = snap.gyroX,
            gyroY = snap.gyroY,
            gyroZ = snap.gyroZ,
            magMagnitude = snap.magMag,
            magX = snap.magX,
            magY = snap.magY,
            magZ = snap.magZ,
            lightLux = snap.lightLux,
            pressureHpa = snap.pressureHpa,
            proximityCm = snap.proximityCm,
            pitchDeg = snap.pitchDeg,
            rollDeg = snap.rollDeg,
            azimuthDeg = snap.azimuthDeg,
            stepCount = snap.stepCount
        )
    }

    // --- Multi-Device Cloud Room Methods ---

    fun createSharedRoom(title: String, metric: HeatmapMetric) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val roomId = sharedRepository.createSharedSession(title, metric.name)
                val session = SharedSessionDoc(
                    id = roomId,
                    title = title,
                    createdBy = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    creatorName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Вы",
                    activeMetric = metric.name
                )
                joinSharedRoom(session)
            } catch (_: Exception) {}
        }
    }

    fun joinSharedRoom(session: SharedSessionDoc) {
        _activeSharedSession.value = session
        // Match room metric
        HeatmapMetric.values().find { it.name == session.activeMetric }?.let {
            _selectedMetric.value = it
        }

        // Listen in real-time to all points submitted by ALL devices in this session!
        sharedPointsSubscriptionJob?.cancel()
        sharedPointsSubscriptionJob = viewModelScope.launch(Dispatchers.Default) {
            sharedRepository.observeSessionPoints(session.id)
                .catch { emit(emptyList()) }
                .collect { pointsDocList ->
                    _sharedPoints.value = pointsDocList

                    // Calculate distinct participating devices
                    val distinctDevices = pointsDocList.map { it.userId }.distinct().size.coerceAtLeast(1)
                    _contributingDevicesCount.value = distinctDevices

                    // Convert cloud docs into unified 3D Heatmap points!
                    val converted = pointsDocList.mapIndexed { idx, doc ->
                        SensorLogPoint(
                            id = (idx + 1).toLong(),
                            sessionId = 999999L,
                            timestamp = doc.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                            latitude = doc.latitude,
                            longitude = doc.longitude,
                            altitude = doc.altitude,
                            accuracy = doc.accuracy.toFloat(),
                            speedKmh = doc.speedKmh.toFloat(),
                            bearing = doc.bearing.toFloat(),
                            relX = doc.relX.toFloat(),
                            relY = doc.relY.toFloat(),
                            accelMagnitude = doc.accelMagnitude.toFloat(),
                            accelX = 0f,
                            accelY = 0f,
                            accelZ = doc.accelMagnitude.toFloat(),
                            gyroMagnitude = doc.gyroMagnitude.toFloat(),
                            gyroX = 0f,
                            gyroY = 0f,
                            gyroZ = doc.gyroMagnitude.toFloat(),
                            magMagnitude = doc.magMagnitude.toFloat(),
                            magX = 0f,
                            magY = 0f,
                            magZ = doc.magMagnitude.toFloat(),
                            lightLux = doc.lightLux.toFloat(),
                            pressureHpa = doc.pressureHpa.toFloat(),
                            proximityCm = 5f,
                            pitchDeg = 0f,
                            rollDeg = 0f,
                            azimuthDeg = doc.bearing.toFloat(),
                            stepCount = 0
                        )
                    }

                    _livePoints.value = converted
                    recomputeGrid()
                }
        }

        _currentTab.value = AppNavTab.HEATMAP_3D
    }

    fun leaveSharedRoom() {
        sharedPointsSubscriptionJob?.cancel()
        sharedPointsSubscriptionJob = null
        _activeSharedSession.value = null
        _sharedPoints.value = emptyList()
        _livePoints.value = emptyList()
        recomputeGrid()
    }

    private fun recomputeGrid() {
        val pts = _livePoints.value
        _gridData.value = HeatmapGridCalculator.computeGrid(pts, _selectedMetric.value)
    }

    fun setTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    fun setMetric(metric: HeatmapMetric) {
        _selectedMetric.value = metric
        recomputeGrid()
    }

    fun setSamplingRate(rate: SamplingRate) {
        _currentSamplingRate.value = rate
        trackingManager.setSamplingRate(rate)
    }

    fun toggleDemoMode(enabled: Boolean) {
        trackingManager.setDemoMode(enabled)
    }

    fun resetOrigin() {
        trackingManager.resetSpatialOrigin()
        if (_activeSharedSession.value == null) {
            _livePoints.value = emptyList()
        }
        recomputeGrid()
    }

    fun startRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            val title = "Сессия #${(sessions.value.size + 1)} (${_selectedMetric.value.titleRu})"
            val id = repository.createSession(title, _selectedMetric.value.titleRu)
            activeLocalSessionId = id
            _activeViewingSessionId.value = id
            uncommittedPoints.clear()
            distanceAccumulator = 0f
            maxRecordedMetric = 0f
            sumRecordedMetric = 0.0
            recordedCount = 0
            if (_activeSharedSession.value == null) {
                _livePoints.value = emptyList()
            }
            _isRecording.value = true
        }
    }

    fun stopRecording() {
        val sid = activeLocalSessionId ?: return
        _isRecording.value = false
        val count = recordedCount
        val maxM = maxRecordedMetric
        val avgM = if (count > 0) (sumRecordedMetric / count).toFloat() else 0f
        val dist = distanceAccumulator

        viewModelScope.launch(Dispatchers.IO) {
            if (uncommittedPoints.isNotEmpty()) {
                val remaining = ArrayList(uncommittedPoints)
                uncommittedPoints.clear()
                repository.saveBatchPoints(remaining)
            }
            repository.finishSession(sid, count, maxM, avgM, dist)
            activeLocalSessionId = null
        }
    }

    fun loadSessionTo3D(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            leaveSharedRoom()
            val points = repository.getPointsForSessionOnce(sessionId)
            _livePoints.value = points
            _activeViewingSessionId.value = sessionId
            _currentTab.value = AppNavTab.HEATMAP_3D
            recomputeGrid()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSession(sessionId)
            if (_activeViewingSessionId.value == sessionId) {
                _activeViewingSessionId.value = null
                _livePoints.value = emptyList()
                recomputeGrid()
            }
        }
    }

    fun runAiAnalysis() {
        if (_isAiLoading.value) return
        _isAiLoading.value = true
        viewModelScope.launch {
            val report = SensorAiService.analyzeTelemetry(
                points = _livePoints.value,
                currentSnapshot = snapshot.value,
                activeMetric = _selectedMetric.value
            )
            _aiReport.value = report
            _isAiLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        samplingCollectorJob?.cancel()
        sharedPointsSubscriptionJob?.cancel()
        trackingManager.cleanUp()
    }
}

private fun SensorLogPoint.extractMetric(metric: HeatmapMetric): Float {
    return HeatmapGridCalculator.extractMetricValue(this, metric)
}

package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthManager
import com.example.auth.AuthScreen
import com.example.ui.ai.AiAnalysisScreen
import com.example.ui.cloud.CloudRoomsScreen
import com.example.ui.heatmap3d.Heatmap3DView
import com.example.ui.sessions.SessionsScreen
import com.example.ui.telemetry.TelemetryDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.SensorViewModel
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = FirebaseAuth.getInstance().currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = FirebaseAuth.getInstance().currentUser
            }
        )
    } else {
        MainScreen(
            onSignOut = {
                AuthManager.signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {
                        currentUser = null
                    },
                    scope = scope
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    viewModel: SensorViewModel = viewModel()
) {
    val context = LocalContext.current

    // Observe ViewModel States
    val snapshot by viewModel.snapshot.collectAsState()
    val hardwareSensors by viewModel.hardwareSensors.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedMetric by viewModel.selectedMetric.collectAsState()
    val gridData by viewModel.gridData.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val currentSamplingRate by viewModel.currentSamplingRate.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeViewingSessionId by viewModel.activeViewingSessionId.collectAsState()
    val aiReport by viewModel.aiReport.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    // Multi-device Cloud States
    val sharedSessions by viewModel.sharedSessions.collectAsState()
    val activeSharedSession by viewModel.activeSharedSession.collectAsState()
    val sharedPoints by viewModel.sharedPoints.collectAsState()
    val contributingDevicesCount by viewModel.contributingDevicesCount.collectAsState()

    val accelHistoryX by viewModel.accelHistoryX.collectAsState()
    val accelHistoryY by viewModel.accelHistoryY.collectAsState()
    val accelHistoryZ by viewModel.accelHistoryZ.collectAsState()

    val magHistoryX by viewModel.magHistoryX.collectAsState()
    val magHistoryY by viewModel.magHistoryY.collectAsState()
    val magHistoryZ by viewModel.magHistoryZ.collectAsState()

    val gyroHistoryX by viewModel.gyroHistoryX.collectAsState()
    val gyroHistoryY by viewModel.gyroHistoryY.collectAsState()
    val gyroHistoryZ by viewModel.gyroHistoryZ.collectAsState()

    // Location Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.trackingManager.startTracking(currentSamplingRate)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // BackHandler: if not on HEATMAP_3D, pressing back returns to HEATMAP_3D
    BackHandler(enabled = currentTab != AppNavTab.HEATMAP_3D) {
        viewModel.setTab(AppNavTab.HEATMAP_3D)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (activeSharedSession != null) Color(0xFF7209B7) else Color(0xFF00F5D4))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SENSORMAP 3D",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeSharedSession != null) "• СЕТЬ ОНЛАЙН" else "• ЛОКАЛЬНО",
                            color = if (activeSharedSession != null) Color(0xFFD0BCFF) else Color(0xFF00F5D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF090D16)
                ),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .testTag("main_top_app_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D1B2A),
                contentColor = Color.White,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppNavTab.HEATMAP_3D,
                    onClick = { viewModel.setTab(AppNavTab.HEATMAP_3D) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "3D Heatmap"
                        )
                    },
                    label = { Text("3D Карта", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF090D16),
                        selectedTextColor = Color(0xFF00F5D4),
                        indicatorColor = Color(0xFF00F5D4),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_heatmap")
                )

                NavigationBarItem(
                    selected = currentTab == AppNavTab.TELEMETRY,
                    onClick = { viewModel.setTab(AppNavTab.TELEMETRY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Датчики"
                        )
                    },
                    label = { Text("Датчики", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF090D16),
                        selectedTextColor = Color(0xFF00F5D4),
                        indicatorColor = Color(0xFF00F5D4),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_telemetry")
                )

                NavigationBarItem(
                    selected = currentTab == AppNavTab.CLOUD_MESH,
                    onClick = { viewModel.setTab(AppNavTab.CLOUD_MESH) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeSharedSession != null) {
                                    Badge(
                                        containerColor = Color(0xFF00F5D4),
                                        contentColor = Color(0xFF090D16)
                                    ) {
                                        Text("$contributingDevicesCount")
                                    }
                                } else if (sharedSessions.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFF7209B7),
                                        contentColor = Color.White
                                    ) {
                                        Text("${sharedSessions.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Мульти-сеть"
                            )
                        }
                    },
                    label = { Text("Мульти-сеть", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF090D16),
                        selectedTextColor = Color(0xFF00F5D4),
                        indicatorColor = Color(0xFF00F5D4),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_cloud_mesh")
                )

                NavigationBarItem(
                    selected = currentTab == AppNavTab.SESSIONS,
                    onClick = { viewModel.setTab(AppNavTab.SESSIONS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Сессии"
                        )
                    },
                    label = { Text("Сессии", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF090D16),
                        selectedTextColor = Color(0xFF00F5D4),
                        indicatorColor = Color(0xFF00F5D4),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_sessions")
                )

                NavigationBarItem(
                    selected = currentTab == AppNavTab.AI_DIAGNOSTICS,
                    onClick = { viewModel.setTab(AppNavTab.AI_DIAGNOSTICS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Анализ"
                        )
                    },
                    label = { Text("AI Анализ", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF090D16),
                        selectedTextColor = Color(0xFF00F5D4),
                        indicatorColor = Color(0xFF00F5D4),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_ai")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppNavTab.HEATMAP_3D -> {
                    Heatmap3DView(
                        snapshot = snapshot,
                        gridData = gridData,
                        selectedMetric = selectedMetric,
                        onMetricSelected = { viewModel.setMetric(it) },
                        isRecording = isRecording,
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { viewModel.stopRecording() },
                        recordedPointsCount = gridData.rawPoints.size,
                        isDemoMode = isDemoMode,
                        onToggleDemoMode = { viewModel.toggleDemoMode(it) },
                        onResetOrigin = { viewModel.resetOrigin() },
                        activeSharedSessionTitle = activeSharedSession?.title,
                        contributingDevicesCount = contributingDevicesCount,
                        onOpenCloudRooms = { viewModel.setTab(AppNavTab.CLOUD_MESH) }
                    )
                }
                AppNavTab.TELEMETRY -> {
                    TelemetryDashboardScreen(
                        snapshot = snapshot,
                        hardwareSensors = hardwareSensors,
                        accelHistoryX = accelHistoryX,
                        accelHistoryY = accelHistoryY,
                        accelHistoryZ = accelHistoryZ,
                        magHistoryX = magHistoryX,
                        magHistoryY = magHistoryY,
                        magHistoryZ = magHistoryZ,
                        gyroHistoryX = gyroHistoryX,
                        gyroHistoryY = gyroHistoryY,
                        gyroHistoryZ = gyroHistoryZ,
                        currentSamplingRate = currentSamplingRate,
                        onSelectSamplingRate = { viewModel.setSamplingRate(it) }
                    )
                }
                AppNavTab.CLOUD_MESH -> {
                    CloudRoomsScreen(
                        sharedSessions = sharedSessions,
                        activeSession = activeSharedSession,
                        sharedPointsCount = sharedPoints.size,
                        contributingDevicesCount = contributingDevicesCount,
                        onCreateRoom = { title, metric -> viewModel.createSharedRoom(title, metric) },
                        onJoinRoom = { session -> viewModel.joinSharedRoom(session) },
                        onLeaveRoom = { viewModel.leaveSharedRoom() },
                        onSignOut = onSignOut,
                        onNavigateTo3D = { viewModel.setTab(AppNavTab.HEATMAP_3D) }
                    )
                }
                AppNavTab.SESSIONS -> {
                    SessionsScreen(
                        sessions = sessions,
                        activeSessionId = activeViewingSessionId,
                        onSelectSessionToView = { viewModel.loadSessionTo3D(it) },
                        onDeleteSession = { viewModel.deleteSession(it) },
                        onExportSession = { sid ->
                            val session = sessions.find { it.id == sid }
                            if (session != null) {
                                val json = JSONObject().apply {
                                    put("sessionId", session.id)
                                    put("title", session.title)
                                    put("startTime", session.startTime)
                                    put("pointCount", session.pointCount)
                                    put("metric", session.primaryMetric)
                                    put("maxMetric", session.maxMetricValue)
                                    put("avgMetric", session.avgMetricValue)
                                    put("distanceMeters", session.distanceMeters)
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SensorSession_${session.id}", json.toString(2))
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Данные сессии #${session.id} скопированы в буфер!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                AppNavTab.AI_DIAGNOSTICS -> {
                    AiAnalysisScreen(
                        report = aiReport,
                        isLoading = isAiLoading,
                        currentMetric = selectedMetric,
                        totalPoints = gridData.rawPoints.size,
                        onRunAnalysis = { viewModel.runAiAnalysis() }
                    )
                }
            }
        }
    }
}

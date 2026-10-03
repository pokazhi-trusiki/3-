package com.example.ui.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firestore.SharedSessionDoc
import com.example.sensor.HeatmapMetric
import com.google.firebase.auth.FirebaseAuth

@Composable
fun CloudRoomsScreen(
    sharedSessions: List<SharedSessionDoc>,
    activeSession: SharedSessionDoc?,
    sharedPointsCount: Int,
    contributingDevicesCount: Int,
    onCreateRoom: (String, HeatmapMetric) -> Unit,
    onJoinRoom: (SharedSessionDoc) -> Unit,
    onLeaveRoom: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateTo3D: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var showCreateDialog by remember { mutableStateOf(false) }
    var newRoomTitle by remember { mutableStateOf("") }
    var selectedMetric by remember { mutableStateOf(HeatmapMetric.MAGNETIC_FIELD) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User Profile & Sign Out Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
            shape = RoundedCornerShape(14.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00F5D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF090D16),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentUser?.displayName ?: "Пользователь сети",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Выйти из аккаунта",
                        tint = Color(0xFFEF476F)
                    )
                }
            }
        }

        // Active Connected Room Banner (if connected)
        activeSession?.let { session ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132A46)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00F5D4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00F5D4))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "АКТИВНАЯ ОБЩАЯ 3D СЕТЬ",
                                color = Color(0xFF00F5D4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Surface(
                            color = Color(0x3300F5D4),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ОНЛАЙН СИНХРОНИЗАЦИЯ",
                                color = Color(0xFF00F5D4),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = session.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Суммарно точек:", color = Color.Gray, fontSize = 10.sp)
                            Text(
                                text = "$sharedPointsCount",
                                color = Color(0xFF00F5D4),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("Устройств в сети:", color = Color.Gray, fontSize = 10.sp)
                            Text(
                                text = "$contributingDevicesCount",
                                color = Color(0xFFFFD166),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("Создатель:", color = Color.Gray, fontSize = 10.sp)
                            Text(
                                text = session.creatorName,
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onLeaveRoom,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF476F)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("leave_room_button")
                        ) {
                            Text("Отключиться", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onNavigateTo3D,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00F5D4),
                                contentColor = Color(0xFF090D16)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_shared_3d_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Открыть 3D Карту", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Header for Available Rooms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ОБЩИЕ 3D СЕССИИ",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7209B7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("create_room_dialog_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Создать комнату", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Room List
        if (sharedSessions.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Пока нет открытых комнат",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Нажмите «Создать комнату», чтобы открыть общее пространство для синхронизации нескольких устройств.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sharedSessions) { room ->
                    val isCurrent = activeSession?.id == room.id

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) Color(0xFF132A46) else Color(0xFF0D1B2A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrent) Color(0xFF00F5D4) else Color(0x2200F5D4)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = room.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Создал: ${room.creatorName} • Метрика: ${room.activeMetric}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                            if (isCurrent) {
                                Surface(
                                    color = Color(0xFF00F5D4),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ПОДКЛЮЧЕН",
                                        color = Color(0xFF090D16),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onJoinRoom(room) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00F5D4),
                                        contentColor = Color(0xFF090D16)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("join_room_${room.id}")
                                ) {
                                    Text("Войти", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Room Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF0D1B2A),
            title = {
                Text(
                    text = "СОЗДАНИЕ ОБЩЕЙ 3D КОМНАТЫ",
                    color = Color(0xFF00F5D4),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Все устройства, подключившиеся к этой комнате, будут в реальном времени передавать свои координаты и показатели датчиков в единую 3D тепловую карту.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = newRoomTitle,
                        onValueChange = { newRoomTitle = it },
                        label = { Text("Название комнаты (напр. Зона 1 - Холл)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5D4),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_title_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (newRoomTitle.isNotBlank()) newRoomTitle else "Общая зона #${sharedSessions.size + 1}"
                        onCreateRoom(title, selectedMetric)
                        showCreateDialog = false
                        newRoomTitle = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4), contentColor = Color(0xFF090D16)),
                    modifier = Modifier.testTag("confirm_create_room_button")
                ) {
                    Text("Создать и войти", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена", color = Color.Gray)
                }
            }
        )
    }
}

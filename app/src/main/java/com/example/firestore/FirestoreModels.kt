package com.example.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

data class UserDoc(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val deviceModel: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SharedSessionDoc(
    @DocumentId val id: String = "",
    val title: String = "",
    val createdBy: String = "",
    val creatorName: String = "",
    val participantCount: Int = 1,
    val activeMetric: String = "MAGNETIC_FIELD",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SharedTelemetryPointDoc(
    @DocumentId val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val deviceId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Double = 0.0,
    val speedKmh: Double = 0.0,
    val bearing: Double = 0.0,
    val relX: Double = 0.0,
    val relY: Double = 0.0,
    val accelMagnitude: Double = 0.0,
    val magMagnitude: Double = 0.0,
    val gyroMagnitude: Double = 0.0,
    val lightLux: Double = 0.0,
    val pressureHpa: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

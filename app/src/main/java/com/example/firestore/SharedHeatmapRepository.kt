package com.example.firestore

import android.content.Context
import android.os.Build
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SharedHeatmapRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing cloud operations.")
    }

    suspend fun syncUserProfile(user: FirebaseUser) {
        val uid = user.uid
        val path = "users/$uid"
        try {
            val userMap = mapOf(
                "userId" to uid,
                "displayName" to (user.displayName ?: "Устройство ${uid.take(4)}"),
                "email" to (user.email ?: ""),
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(userMap).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    fun observeSharedSessions(): Flow<List<SharedSessionDoc>> = flow {
        val path = "shared_sessions"
        emitAll(
            db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SharedSessionDoc::class.java)
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    suspend fun createSharedSession(title: String, activeMetric: String): String {
        val uid = requireUserId()
        val user = auth.currentUser
        val displayName = user?.displayName ?: "Пользователь ${uid.take(4)}"
        val docRef = db.collection("shared_sessions").document()
        val path = docRef.path

        val payload = mapOf(
            "id" to docRef.id,
            "title" to title,
            "createdBy" to uid,
            "creatorName" to displayName,
            "participantCount" to 1,
            "activeMetric" to activeMetric,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    fun observeSessionPoints(sessionId: String): Flow<List<SharedTelemetryPointDoc>> = flow {
        if (sessionId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "shared_sessions/$sessionId/points"
        emitAll(
            db.collection("shared_sessions")
                .document(sessionId)
                .collection("points")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(500)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SharedTelemetryPointDoc::class.java)
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    suspend fun contributePoint(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        accuracy: Float,
        speedKmh: Float,
        bearing: Float,
        relX: Float,
        relY: Float,
        accelMag: Float,
        magMag: Float,
        gyroMag: Float,
        lightLux: Float,
        pressureHpa: Float
    ) {
        val uid = requireUserId()
        val user = auth.currentUser
        val displayName = user?.displayName ?: "Устройство ${uid.take(4)}"
        val docRef = db.collection("shared_sessions")
            .document(sessionId)
            .collection("points")
            .document()
        val path = docRef.path

        val payload = mapOf(
            "id" to docRef.id,
            "sessionId" to sessionId,
            "userId" to uid,
            "userDisplayName" to displayName,
            "deviceId" to "${Build.MANUFACTURER}_${Build.MODEL}",
            "latitude" to latitude,
            "longitude" to longitude,
            "altitude" to altitude,
            "accuracy" to accuracy.toDouble(),
            "speedKmh" to speedKmh.toDouble(),
            "bearing" to bearing.toDouble(),
            "relX" to relX.toDouble(),
            "relY" to relY.toDouble(),
            "accelMagnitude" to accelMag.toDouble(),
            "magMagnitude" to magMag.toDouble(),
            "gyroMagnitude" to gyroMag.toDouble(),
            "lightLux" to lightLux.toDouble(),
            "pressureHpa" to pressureHpa.toDouble(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }
}

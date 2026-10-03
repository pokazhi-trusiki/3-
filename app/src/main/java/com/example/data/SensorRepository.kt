package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Query("SELECT * FROM sensor_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SensorSession>>

    @Query("SELECT * FROM sensor_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): SensorSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SensorSession): Long

    @Update
    suspend fun updateSession(session: SensorSession)

    @Query("DELETE FROM sensor_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM sensor_points WHERE sessionId = :sessionId")
    suspend fun deletePointsForSession(sessionId: Long)

    @Query("SELECT * FROM sensor_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getPointsForSession(sessionId: Long): Flow<List<SensorLogPoint>>

    @Query("SELECT * FROM sensor_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSessionOnce(sessionId: Long): List<SensorLogPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<SensorLogPoint>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: SensorLogPoint)
}

@Database(entities = [SensorSession::class, SensorLogPoint::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sensormap_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class SensorRepository(private val dao: SensorDao) {
    val allSessions: Flow<List<SensorSession>> = dao.getAllSessions()

    fun getPointsForSession(sessionId: Long): Flow<List<SensorLogPoint>> =
        dao.getPointsForSession(sessionId)

    suspend fun getPointsForSessionOnce(sessionId: Long): List<SensorLogPoint> =
        dao.getPointsForSessionOnce(sessionId)

    suspend fun createSession(title: String, metric: String): Long {
        return dao.insertSession(
            SensorSession(
                title = title,
                startTime = System.currentTimeMillis(),
                primaryMetric = metric
            )
        )
    }

    suspend fun saveBatchPoints(points: List<SensorLogPoint>) {
        if (points.isNotEmpty()) {
            dao.insertPoints(points)
        }
    }

    suspend fun finishSession(
        sessionId: Long,
        pointCount: Int,
        maxMetric: Float,
        avgMetric: Float,
        distanceMeters: Float
    ) {
        val existing = dao.getSessionById(sessionId) ?: return
        dao.updateSession(
            existing.copy(
                endTime = System.currentTimeMillis(),
                pointCount = pointCount,
                maxMetricValue = maxMetric,
                avgMetricValue = avgMetric,
                distanceMeters = distanceMeters
            )
        )
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deletePointsForSession(sessionId)
        dao.deleteSession(sessionId)
    }
}

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.VoiceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceLogDao {
    @Query("SELECT * FROM voice_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogs(): Flow<List<VoiceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VoiceLog): Long

    @Query("DELETE FROM voice_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM voice_logs")
    suspend fun clearAllLogs()
}

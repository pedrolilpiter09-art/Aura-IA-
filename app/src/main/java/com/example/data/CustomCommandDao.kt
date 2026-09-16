package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CustomCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCommandDao {
    @Query("SELECT * FROM custom_commands ORDER BY createdAt DESC")
    fun getAllCommands(): Flow<List<CustomCommand>>

    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1")
    suspend fun getActiveCommands(): List<CustomCommand>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CustomCommand): Long

    @Update
    suspend fun updateCommand(command: CustomCommand)

    @Delete
    suspend fun deleteCommand(command: CustomCommand)

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteCommandById(id: Long)
}

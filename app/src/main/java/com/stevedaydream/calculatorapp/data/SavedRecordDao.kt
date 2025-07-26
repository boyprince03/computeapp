package com.stevedaydream.calculatorapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRecordDao {
    @Query("SELECT * FROM saved_records ORDER BY time DESC")
    fun getAll(): Flow<List<SavedRecord>>

    @Insert
    suspend fun insert(record: SavedRecord)
}

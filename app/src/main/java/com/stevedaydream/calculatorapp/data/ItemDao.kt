package com.stevedaydream.calculatorapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAll(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Int): Item?

    @Query("SELECT * FROM items WHERE department = :department AND category = :category AND name = :name LIMIT 1")
    suspend fun findDuplicate(department: String, category: String, name: String): Item?


    @Insert
    suspend fun insert(item: Item)

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)
}

package com.stevedaydream.calculatorapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val department: String,
    val category: String,
    val name: String,
    val price: Int
)

package com.stevedaydream.calculatorapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_records")
data class SavedRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: Long,
    val items: String,      // JSON 格式存 {名稱:數量, ...}
    val total: Int,
    val barcode: String? = null    // 新增這一行
)

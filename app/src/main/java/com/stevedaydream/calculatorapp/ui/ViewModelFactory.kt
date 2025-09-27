package com.stevedaydream.calculatorapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stevedaydream.calculatorapp.data.ItemDao
import com.stevedaydream.calculatorapp.data.SavedRecordDao

class ViewModelFactory(
    private val itemDao: ItemDao,
    private val recordDao: SavedRecordDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(itemDao, recordDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
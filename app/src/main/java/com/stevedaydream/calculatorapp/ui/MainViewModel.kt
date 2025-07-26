package com.stevedaydream.calculatorapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stevedaydream.calculatorapp.data.AppDatabase
import com.stevedaydream.calculatorapp.data.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).itemDao()

    val items = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            dao.delete(item)
        }
    }
}

package com.stevedaydream.calculatorapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stevedaydream.calculatorapp.data.Item
import com.stevedaydream.calculatorapp.data.ItemDao
import com.stevedaydream.calculatorapp.data.SavedRecord
import com.stevedaydream.calculatorapp.data.SavedRecordDao
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CalculatorViewModel(
    private val itemDao: ItemDao,
    private val recordDao: SavedRecordDao
) : ViewModel() {

    // --- StateFlows for UI State ---
    private val _barcodeText = MutableStateFlow("")
    val barcodeText: StateFlow<String> = _barcodeText.asStateFlow()

    private val _selectedDepartment = MutableStateFlow("")
    val selectedDepartment: StateFlow<String> = _selectedDepartment.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val selectedCounts: StateFlow<Map<Int, Int>> = _selectedCounts.asStateFlow()

    private val _saveMsg = MutableStateFlow<String?>(null)
    val saveMsg: StateFlow<String?> = _saveMsg.asStateFlow()

    // --- Derived State from other flows ---
    val allItems: StateFlow<List<Item>> = itemDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val departmentList: StateFlow<List<String>> = allItems.map { items ->
        items.map { it.department }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryList: StateFlow<List<String>> = combine(
        selectedDepartment,
        allItems
    ) { department, items ->
        if (department.isNotBlank()) {
            items.filter { it.department == department }.map { it.category }.distinct()
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredItems: StateFlow<List<Item>> = combine(
        selectedDepartment,
        selectedCategory,
        allItems
    ) { department, category, items ->
        items.filter {
            (department.isBlank() || it.department == department) &&
                    (category.isBlank() || it.category == category)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedList: StateFlow<List<Item>> = combine(selectedCounts, allItems) { counts, items ->
        items.filter { (counts[it.id] ?: 0) > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val totalPrice: StateFlow<Int> = combine(selectedCounts, allItems) { counts, items ->
        items.sumOf { item ->
            val count = counts[item.id] ?: 0
            item.price * count
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)


    // --- Events from UI ---

    fun onBarcodeTextChange(text: String) {
        _barcodeText.value = text
    }

    fun onDepartmentSelected(department: String) {
        _selectedDepartment.value = department
        _selectedCategory.value = "" // Reset category when department changes
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onItemCheckedChange(item: Item, isChecked: Boolean) {
        val newCounts = _selectedCounts.value.toMutableMap()
        if (isChecked) {
            newCounts[item.id] = newCounts[item.id]?.coerceAtLeast(1) ?: 1
        } else {
            newCounts[item.id] = 0
        }
        _selectedCounts.value = newCounts
    }

    fun onItemCountChange(item: Item, change: Int) {
        val newCounts = _selectedCounts.value.toMutableMap()
        val currentCount = newCounts[item.id] ?: 0
        newCounts[item.id] = (currentCount + change).coerceAtLeast(0)
        _selectedCounts.value = newCounts
    }

    fun resetSelections() {
        _selectedCounts.value = emptyMap()
        _selectedCategory.value = ""
        _barcodeText.value = ""
        _saveMsg.value = null
    }

    fun clearSaveMsg() {
        _saveMsg.value = null
    }

    fun saveRecord() {
        if (selectedList.value.isEmpty()) {
            _saveMsg.value = "⚠️ 請先選取項目"
            return
        }

        viewModelScope.launch {
            try {
                val itemsMap = selectedList.value.associate { it.name to (_selectedCounts.value[it.id] ?: 0) }
                val record = SavedRecord(
                    time = System.currentTimeMillis(),
                    items = Gson().toJson(itemsMap),
                    total = totalPrice.value,
                    barcode = _barcodeText.value.ifBlank { null }
                )
                recordDao.insert(record)
                _saveMsg.value = "✅ 資料已儲存！"
                resetSelections() // 儲存後重置
            } catch (e: Exception) {
                _saveMsg.value = "❌ 儲存失敗：${e.localizedMessage}"
            }
        }
    }
}
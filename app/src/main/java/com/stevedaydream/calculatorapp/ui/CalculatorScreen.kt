package com.stevedaydream.calculatorapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stevedaydream.calculatorapp.data.ItemDao
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stevedaydream.calculatorapp.data.SavedRecordDao

@Composable
fun CalculatorScreen(
    dao: ItemDao,
    recordDao: SavedRecordDao,
    onManageClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    // 使用 viewModel() 輔助函式來取得 ViewModel 實例
    val viewModel: CalculatorViewModel = viewModel(
        factory = ViewModelFactory(dao, recordDao)
    )

    // 從 ViewModel 收集狀態
    val barcodeText by viewModel.barcodeText.collectAsState()
    val selectedDepartment by viewModel.selectedDepartment.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val departmentList by viewModel.departmentList.collectAsState()
    val categoryList by viewModel.categoryList.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val selectedCounts by viewModel.selectedCounts.collectAsState()
    val selectedList by viewModel.selectedList.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()
    val saveMsg by viewModel.saveMsg.collectAsState()

    val allRecords by recordDao.getAll().collectAsState(initial = emptyList())


    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result: ScanIntentResult ->
        result.contents?.let { viewModel.onBarcodeTextChange(it) }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ... (標題與管理資料按鈕 - 這部分不變) ...
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "項目計算",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onManageClick() }) {
                Text("管理資料")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))


        BarcodeInputRow(
            barcodeText = barcodeText,
            onBarcodeTextChange = { viewModel.onBarcodeTextChange(it) },
            onScanClick = {
                scanLauncher.launch(
                    ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                        setPrompt("請對準條碼")
                        setCameraId(0)
                        setBeepEnabled(true)
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        DropdownSelector(
            label = "科別",
            options = departmentList,
            selectedOption = selectedDepartment,
            onOptionSelected = { viewModel.onDepartmentSelected(it) }
        )

        DropdownSelector(
            label = "類別",
            options = categoryList,
            selectedOption = selectedCategory,
            onOptionSelected = { viewModel.onCategorySelected(it) },
            enabled = selectedDepartment.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 表單與清單區
        Column {
            if (selectedDepartment.isNotBlank()) {
                Text(
                    "✅ 可選項目：${if (selectedCategory.isNotBlank()) "$selectedDepartment / $selectedCategory" else selectedDepartment}"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        // ... (暫無項目 - 這部分不變) ...
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暫無項目",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredItems) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = (selectedCounts[item.id] ?: 0) > 0,
                                            onCheckedChange = { isChecked ->
                                                viewModel.onItemCheckedChange(item, isChecked)
                                            }
                                        )
                                        Text("${item.name} - \$${item.price}")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.onItemCountChange(item, -1) }) { Text("-") }
                                        Text("${selectedCounts[item.id] ?: 0}")
                                        IconButton(onClick = { viewModel.onItemCountChange(item, 1) }) { Text("+") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 已選清單
            if (selectedList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("📝 已選清單：", style = MaterialTheme.typography.titleMedium)
                selectedList.forEach { item ->
                    val count = selectedCounts[item.id] ?: 0
                    if (count > 0) {
                        Text("${item.name} × $count = \$${item.price * count}")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // 新增儲存 Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💰 總金額：$totalPrice", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = { viewModel.resetSelections() }) {
                Text("重置")
            }
            Button(onClick = { viewModel.saveRecord() }) {
                Text("儲存資料")
            }
        }
        saveMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
        // ... (歷史紀錄部分不變) ...
        if (allRecords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("歷史儲存紀錄：", style = MaterialTheme.typography.titleMedium)
            // 只顯示最近5筆
            allRecords.take(5).forEach { record ->
                val itemsMap: Map<String, Int> = remember(record.items) {
                    com.google.gson.Gson().fromJson<Map<String, Int>>(record.items, object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type)
                }
                Text(
                    "🕒 ${
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(record.time))
                    }  條碼:${record.barcode ?: "-"} 共${itemsMap.values.sum()}項, 金額：${record.total}"
                )
            }
        }
    }
}

// ... (BarcodeInputRow 和 DropdownSelector 函式不變) ...
// (請保留您原有的 BarcodeInputRow 和 DropdownSelector 程式碼)
@Composable
fun BarcodeInputRow(
    barcodeText: String,
    onBarcodeTextChange: (String) -> Unit,
    onScanClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = barcodeText,
            onValueChange = onBarcodeTextChange,
            label = { Text("條碼") },
            modifier = Modifier.weight(1f),
            trailingIcon = {
                if (barcodeText.isNotBlank()) {
                    IconButton(onClick = { onBarcodeTextChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "清除")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onScanClick) {
            Text("掃描條碼")
        }
    }
}

@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        // Text(label) // 標籤可以整合進 OutlinedTextField
        Box {
            OutlinedTextField(
                value = selectedOption.ifBlank { "請選擇" },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
                enabled = enabled,
                label = { Text(label) }, // 將 label 放在這裡
                trailingIcon = {
                    IconButton(
                        onClick = { if (enabled) expanded = true },
                        enabled = enabled
                    ) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            )
            DropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth().heightIn(max=300.dp)
            ) {
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "無選項",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        onClick = {}
                    )
                } else {
                    options.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                onOptionSelected(it)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
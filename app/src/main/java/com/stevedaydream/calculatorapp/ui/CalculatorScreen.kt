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
import com.stevedaydream.calculatorapp.data.SavedRecordDao
import kotlinx.coroutines.launch
import com.stevedaydream.calculatorapp.data.SavedRecord
import java.util.Date
import com.google.gson.Gson

@Composable
fun CalculatorScreen(
    dao: ItemDao,
    recordDao: SavedRecordDao,
    onManageClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val allItems by dao.getAll().collectAsState(initial = emptyList())

    var selectedDepartment by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedCounts by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var barcodeText by remember { mutableStateOf("") }

    val departmentList = allItems.map { it.department }.distinct()
    val categoryList = if (selectedDepartment.isNotBlank()) {
        allItems.filter { it.department == selectedDepartment }
            .map { it.category }
            .distinct()
    } else emptyList()

    val filteredItems = allItems.filter {
        (selectedDepartment.isBlank() || it.department == selectedDepartment) &&
                (selectedCategory.isBlank() || it.category == selectedCategory)
    }

    val totalPrice = filteredItems.sumOf { item ->
        val count = selectedCounts[item.id] ?: 0
        item.price * count
    }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result: ScanIntentResult ->
        result.contents?.let { barcodeText = it }
    }

    val coroutineScope = rememberCoroutineScope()
    var saveMsg by remember { mutableStateOf<String?>(null) }
    val selectedList = filteredItems.filter { (selectedCounts[it.id] ?: 0) > 0 }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        // -------- 標題與管理資料按鈕 --------
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
            onBarcodeTextChange = { barcodeText = it },
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
            onOptionSelected = {
                selectedDepartment = it
                selectedCategory = ""
                selectedCounts = mutableMapOf()
            }
        )

        DropdownSelector(
            label = "類別",
            options = categoryList,
            selectedOption = selectedCategory,
            onOptionSelected = {
                selectedCategory = it
                selectedCounts = mutableMapOf()
            },
            enabled = selectedDepartment.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 表單與清單區
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f, fill = false)
        ) {
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
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedCounts[item.id]?.let { it > 0 } ?: false,
                                            onCheckedChange = { checked ->
                                                selectedCounts = selectedCounts.toMutableMap().apply {
                                                    if (checked) {
                                                        this[item.id] = this[item.id]?.coerceAtLeast(1) ?: 1
                                                    } else {
                                                        this[item.id] = 0
                                                    }
                                                }
                                            }
                                        )
                                        Text("${item.name} - \$${item.price}")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            selectedCounts = selectedCounts.toMutableMap().apply {
                                                val now = (this[item.id] ?: 1)
                                                if (now > 1) this[item.id] = now - 1
                                            }
                                        }) { Text("-") }
                                        Text("${selectedCounts[item.id]?.takeIf { it > 0 } ?: 1}")
                                        IconButton(onClick = {
                                            selectedCounts = selectedCounts.toMutableMap().apply {
                                                val now = (this[item.id] ?: 0)
                                                this[item.id] = now + 1
                                            }
                                        }) { Text("+") }
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
            TextButton(onClick = {
                selectedCounts = mutableMapOf()
                selectedCategory = ""
            }) {
                Text("重置")
            }
            Button(onClick = {
                if (selectedList.isNotEmpty()) {
                    coroutineScope.launch {
                        try {
                            val itemsMap = selectedList.associate { it.name to (selectedCounts[it.id] ?: 0) }
                            val record = SavedRecord(
                                time = System.currentTimeMillis(),
                                items = Gson().toJson(itemsMap),
                                total = totalPrice
                            )
                            recordDao.insert(record)
                            saveMsg = "✅ 資料已儲存！"
                        } catch (e: Exception) {
                            saveMsg = "❌ 儲存失敗：${e.localizedMessage}"
                        }
                    }
                } else {
                    saveMsg = "⚠️ 請先選取項目"
                }
            }) {
                Text("儲存資料")
            }
        }
        saveMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        val allRecords by recordDao.getAll().collectAsState(initial = emptyList())
        if (allRecords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("歷史儲存紀錄：", style = MaterialTheme.typography.titleMedium)
            allRecords.forEach { record ->
                val itemsMap = Gson().fromJson<Map<String, Int>>(record.items, Map::class.java)
                Text(
                    "🕒 ${
                        java.text.SimpleDateFormat("HH:mm:ss").format(Date(record.time))
                    }  共${itemsMap.values.sum()}項, 金額：${record.total}"
                )
            }
        }
    }
}

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
        Text(label)
        Box {
            OutlinedTextField(
                value = selectedOption.ifBlank { "請選擇" },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
                enabled = enabled,
                label = { Text("請選擇") },
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
                modifier = Modifier.heightIn(max = 300.dp)
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

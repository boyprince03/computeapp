package com.stevedaydream.calculatorapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.stevedaydream.calculatorapp.data.Item
import com.stevedaydream.calculatorapp.data.ItemDao
import com.stevedaydream.calculatorapp.data.SavedRecordDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    dao: ItemDao,
    recordDao: SavedRecordDao,
    onManageClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val viewModel: CalculatorViewModel = viewModel(factory = ViewModelFactory(dao, recordDao))

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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result: ScanIntentResult ->
        result.contents?.let { viewModel.onBarcodeTextChange(it) }
    }

    LaunchedEffect(saveMsg) {
        saveMsg?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSaveMsg()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("項目計算機") },
                actions = {
                    TextButton(onClick = onManageClick) {
                        Text("管理資料")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.saveRecord() },
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = "儲存") },
                text = { Text("儲存紀錄") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // --- ✅ 固定的頂部區塊 ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                TotalAmountCard(totalPrice, onReset = { viewModel.resetSelections() })

                // 只有當清單不為空時才顯示
                if (selectedList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectedItemsCard(
                        selectedList = selectedList,
                        selectedCounts = selectedCounts
                    )
                }
            }

            // --- ✅ 可滾動的內容區塊 ---
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                // 條碼與篩選
                item {
                    FilterCard(
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
                        },
                        departmentList = departmentList,
                        selectedDepartment = selectedDepartment,
                        onDepartmentSelected = { viewModel.onDepartmentSelected(it) },
                        categoryList = categoryList,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.onCategorySelected(it) }
                    )
                }

                // 可選項目列表
                if (selectedDepartment.isNotBlank()) {
                    item {
                        Text(
                            "可選項目：${if (selectedCategory.isNotBlank()) "$selectedDepartment / $selectedCategory" else selectedDepartment}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "此分類下暫無項目",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            ItemSelectionRow(
                                item = item,
                                count = selectedCounts[item.id] ?: 0,
                                onCheckedChange = { isChecked -> viewModel.onItemCheckedChange(item, isChecked) },
                                onCountChange = { change -> viewModel.onItemCountChange(item, change) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TotalAmountCard(totalPrice: Int, onReset: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "總金額",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "NT$ $totalPrice",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onReset) {
                Text("全部重置")
            }
        }
    }
}

@Composable
fun FilterCard(
    barcodeText: String,
    onBarcodeTextChange: (String) -> Unit,
    onScanClick: () -> Unit,
    departmentList: List<String>,
    selectedDepartment: String,
    onDepartmentSelected: (String) -> Unit,
    categoryList: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            BarcodeInputRow(
                barcodeText = barcodeText,
                onBarcodeTextChange = onBarcodeTextChange,
                onScanClick = onScanClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(
                label = "科別",
                options = departmentList,
                selectedOption = selectedDepartment,
                onOptionSelected = onDepartmentSelected
            )
            Spacer(modifier = Modifier.height(8.dp))
            DropdownSelector(
                label = "類別",
                options = categoryList,
                selectedOption = selectedCategory,
                onOptionSelected = onCategorySelected,
                enabled = selectedDepartment.isNotBlank()
            )
        }
    }
}

@Composable
fun ItemSelectionRow(
    item: Item,
    count: Int,
    onCheckedChange: (Boolean) -> Unit,
    onCountChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = count > 0,
            onCheckedChange = onCheckedChange
        )
        Text(
            "${item.name} - \$${item.price}",
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onCountChange(-1) }, enabled = count > 0) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "$count",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onCountChange(1) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}


// ✅ 修改 SelectedItemsCard 以處理長清單
@Composable
fun SelectedItemsCard(selectedList: List<Item>, selectedCounts: Map<Int, Int>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("已選清單", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // ✅ 加上最大高度限制和垂直滾動
            Column(
                modifier = Modifier
                    .heightIn(max = 150.dp) // 可根據需求調整最大高度
                    .verticalScroll(rememberScrollState())
            ) {
                selectedList.forEach { item ->
                    val count = selectedCounts[item.id] ?: 0
                    if (count > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.name} × $count", modifier = Modifier.weight(1f))
                            Text("\$${item.price * count}")
                        }
                    }
                }
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
            label = { Text("條碼 (選填)") },
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
            Text("掃描")
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
    Box {
        OutlinedTextField(
            value = selectedOption.ifBlank { "請選擇" },
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
            enabled = enabled,
            label = { Text(label) },
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
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
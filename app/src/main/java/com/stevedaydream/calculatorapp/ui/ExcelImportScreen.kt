package com.stevedaydream.calculatorapp.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stevedaydream.calculatorapp.data.Item
import com.stevedaydream.calculatorapp.data.ItemDao
import jxl.Workbook
import jxl.read.biff.BiffException
import kotlinx.coroutines.launch
import java.io.InputStream

@Composable
fun ExcelImportScreen(
    dao: ItemDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var parsedItems by remember { mutableStateOf<List<Item>>(emptyList()) }
    var fileName by remember { mutableStateOf<String?>(null) }

    // 匯入狀態提示
    var importResult by remember { mutableStateOf("") }
    var duplicateList by remember { mutableStateOf<List<Item>>(emptyList()) }
    var uniqueList by remember { mutableStateOf<List<Item>>(emptyList()) }

    // Excel 選取 launcher
    val excelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                fileName = getFileName(context, it)
                val inputStream = context.contentResolver.openInputStream(it)
                parsedItems = inputStream?.let { parseExcel(it) } ?: emptyList()
                // reset
                duplicateList = emptyList()
                uniqueList = emptyList()
                importResult = ""
            }
        }
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text("📥 Excel 匯入", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { excelPicker.launch("*/*")  // 先用所有檔案測試
        }) {
            Text("選擇 Excel 檔")
        }

        fileName?.let {
            Text("已選檔案：$it", modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (parsedItems.isNotEmpty()) {
            Text("預覽資料（${parsedItems.size} 筆）：", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(parsedItems) { item ->
                    Text("${item.department} | ${item.category} | ${item.name} | ${item.price}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 加入檢查重複的按鈕
            Button(onClick = {
                scope.launch {
                    val existList = mutableListOf<Item>()
                    val nonExistList = mutableListOf<Item>()
                    for (item in parsedItems) {
                        val exist = dao.findDuplicate(item.department, item.category, item.name)
                        if (exist != null) existList.add(item)
                        else nonExistList.add(item)
                    }
                    duplicateList = existList
                    uniqueList = nonExistList
                    importResult = ""
                }
            }) {
                Text("🔍 檢查是否有重複資料")
            }

            // 檢查結果提示
            if (duplicateList.isNotEmpty() || uniqueList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                if (duplicateList.isNotEmpty()) {
                    Text("❌ 偵測到重複資料（共 ${duplicateList.size} 筆，將不匯入）：", color = MaterialTheme.colorScheme.error)
                    LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                        items(duplicateList) { item ->
                            Text("${item.department} | ${item.category} | ${item.name}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (uniqueList.isNotEmpty()) {
                    Text("✅ 可匯入新資料：${uniqueList.size} 筆")
                }
            }

            // 匯入按鈕
            if (uniqueList.isNotEmpty()) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                uniqueList.forEach { dao.insert(it) }
                                importResult = "已成功匯入 ${uniqueList.size} 筆資料！"
                                // 清空狀態
                                parsedItems = emptyList()
                                fileName = null
                                duplicateList = emptyList()
                                uniqueList = emptyList()
                                onBack() // 匯入完自動返回
                            } catch (e: Exception) {
                                importResult = "❌ 匯入失敗：${e.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text("✅ 匯入可新增資料")
                }
            }

            // 匯入後提示
            if (importResult.isNotBlank()) {
                Text(importResult, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回")
        }
    }
}


fun parseExcel(input: InputStream): List<Item> {
    val items = mutableListOf<Item>()
    try {
        val workbook = Workbook.getWorkbook(input)
        val sheet = workbook.getSheet(0)

        for (i in 1 until sheet.rows) { // 跳過標題列
            val department = sheet.getCell(0, i).contents
            val category = sheet.getCell(1, i).contents
            val name = sheet.getCell(2, i).contents
            val price = sheet.getCell(3, i).contents.toIntOrNull() ?: 0

            if (department.isNotBlank() && category.isNotBlank() && name.isNotBlank()) {
                items.add(Item(department = department, category = category, name = name, price = price))
            }
        }
    } catch (e: BiffException) {
        e.printStackTrace()
    }
    return items
}

fun getFileName(context: android.content.Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) return cursor.getString(nameIndex)
    }
    return uri.lastPathSegment
}

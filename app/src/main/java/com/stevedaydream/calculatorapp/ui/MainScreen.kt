package com.stevedaydream.calculatorapp.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.stevedaydream.calculatorapp.data.Item
import com.stevedaydream.calculatorapp.data.ItemDao
import com.stevedaydream.calculatorapp.data.SavedRecordDao
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dao: ItemDao,
    recordDao: SavedRecordDao,
    navController: NavHostController,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    onDeleteClick: (Item) -> Unit,
    navToImport: () -> Unit,
    onHistoryClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val items by dao.getAll().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel"),
        onResult = { uri: Uri? ->
            if (uri != null) {
                exportItemsToExcelJxlSAF(context, items, uri)
            } else {
                Toast.makeText(context, "未選擇儲存位置", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 資料管理") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "選單")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("➕ 新增資料") },
                            onClick = {
                                menuExpanded = false
                                onAddClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📥 匯入 Excel") },
                            onClick = {
                                menuExpanded = false
                                navToImport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⬇️ 匯出 Excel 備份") },
                            onClick = {
                                menuExpanded = false
                                exportLauncher.launch("項目資料備份_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xls")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("\uD83D\uDD19 返回計算頁面") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("calculator")
                            }
                        )

                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp) // 水平 padding
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(8.dp)) // 頂部間距
            Button(
                onClick = { onHistoryClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("查看歷史紀錄")
            }
            Spacer(modifier = Modifier.height(12.dp)) // 按鈕下方間距

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp) // 每個卡片之間的垂直間距
            ) {
                items(items) { item ->
                    ItemRow(
                        item = item,
                        onEdit = { onEditClick(item.id) },
                        onDelete = { onDeleteClick(item) }
                    )
                }
            }
        }
    }
}

/**
 * 美化後的項目卡片 Composable
 */
@Composable
fun ItemRow(item: Item, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 增加陰影
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左側：項目資訊
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.department} / ${item.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // 使用次要顏色
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${item.price}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary // 使用主題強調色
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 右側：操作按鈕
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onEdit) {
                    Text("編輯")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    // 讓邊框和文字顏色都使用錯誤狀態的顏色
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("刪除")
                }
            }
        }
    }
}


fun exportItemsToExcelJxlSAF(context: Context, items: List<Item>, uri: android.net.Uri) {
    try {
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw Exception("無法取得儲存位置")

        val workbook: jxl.write.WritableWorkbook = jxl.Workbook.createWorkbook(outputStream)
        val sheet: jxl.write.WritableSheet = workbook.createSheet("Items", 0)

        // 標題
        sheet.addCell(jxl.write.Label(0, 0, "科別"))
        sheet.addCell(jxl.write.Label(1, 0, "類別"))
        sheet.addCell(jxl.write.Label(2, 0, "名稱"))
        sheet.addCell(jxl.write.Label(3, 0, "金額"))

        // 資料
        for ((i, item) in items.withIndex()) {
            sheet.addCell(jxl.write.Label(0, i + 1, item.department))
            sheet.addCell(jxl.write.Label(1, i + 1, item.category))
            sheet.addCell(jxl.write.Label(2, i + 1, item.name))
            sheet.addCell(jxl.write.Label(3, i + 1, item.price.toString()))
        }

        workbook.write()
        workbook.close()
        outputStream.close()

        Toast.makeText(context, "匯出成功！", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "匯出失敗：${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}
package com.stevedaydream.calculatorapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.stevedaydream.calculatorapp.data.Item
import com.stevedaydream.calculatorapp.data.ItemDao
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.stevedaydream.calculatorapp.data.SavedRecordDao
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import jxl.Workbook
import jxl.write.Label
import jxl.write.WritableSheet
import jxl.write.WritableWorkbook


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dao: ItemDao,
    recordDao: SavedRecordDao, // 新增
    navController: NavHostController,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    onDeleteClick: (Item) -> Unit,
    navToImport: () -> Unit,
    onHistoryClick: ()->Unit
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
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Button(
                onClick = { onHistoryClick() }, // 傳遞 lambda
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("查看歷史紀錄")
            }
            LazyColumn {
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

// ✅ 放在 MainScreen 外面
@Composable
fun ItemRow(item: Item, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("科別：${item.department}")
            Text("類別：${item.category}")
            Text("名稱：${item.name}")
            Text("金額：${item.price}")

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onEdit) {
                    Text("編輯")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

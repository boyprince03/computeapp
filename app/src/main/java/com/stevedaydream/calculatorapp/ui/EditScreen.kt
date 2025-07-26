package com.stevedaydream.calculatorapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stevedaydream.calculatorapp.data.Item

@Composable
fun EditScreen(
    item: Item? = null,
    onSave: (Item) -> Unit,
    onBack: () -> Unit
) {
    var department by remember { mutableStateOf(item?.department ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "") }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var priceText by remember { mutableStateOf(item?.price?.toString() ?: "") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text(if (item == null) "➕ 新增項目" else "✏️ 編輯項目", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("科別") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("類別") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名稱") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("金額") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack) {
                Text("返回")
            }
            Button(onClick = {
                if (department.isBlank() || category.isBlank() || name.isBlank() || priceText.isBlank()) {
                    // 可加一個 Dialog 或 Toast
                    return@Button
                }
                val price = priceText.toIntOrNull() ?: 0
                onSave(
                    Item(
                        id = item?.id ?: 0,
                        department = department,
                        category = category,
                        name = name,
                        price = price
                    )
                )
            }) {
                Text("儲存")
            }

        }
    }
}

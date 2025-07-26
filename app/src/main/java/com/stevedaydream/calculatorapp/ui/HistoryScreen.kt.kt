package com.stevedaydream.calculatorapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stevedaydream.calculatorapp.data.SavedRecordDao
import java.util.*
import com.google.gson.Gson
import com.stevedaydream.calculatorapp.data.SavedRecord
import com.google.gson.reflect.TypeToken

@Composable
fun HistoryScreen(
    recordDao: SavedRecordDao,
    onBack: () -> Unit = {}
) {
    val allRecords by recordDao.getAll().collectAsState(initial = emptyList())
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text("歷史儲存紀錄", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (allRecords.isEmpty()) {
            Text("暫無紀錄")
        } else {
            LazyColumn {
                items(allRecords) { record ->
                    RecordRow(record)
                }
            }
        }
    }
}

@Composable
fun RecordRow(record: SavedRecord) {
    val itemsMap: Map<String, Int> = remember(record.items) {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        Gson().fromJson<Map<String, Int>>(record.items, type)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🕒 ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(record.time))}")
            itemsMap.forEach { (name, count) ->
                Text("　- $name x $count")
            }
            Text("總金額：${record.total}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

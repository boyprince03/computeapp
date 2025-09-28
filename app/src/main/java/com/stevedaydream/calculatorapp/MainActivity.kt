package com.stevedaydream.calculatorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.stevedaydream.calculatorapp.data.AppDatabase
import com.stevedaydream.calculatorapp.navigation.AppNavGraph
import com.stevedaydream.calculatorapp.ui.FooterInfo // 👈 匯入 FooterInfo
import com.stevedaydream.calculatorapp.ui.theme.CalculatorAppTheme // 👈 匯入您的主題
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val itemDao = db.itemDao()
        val savedRecordDao = db.savedRecordDao()

        setContent {
            // ✅ 使用您的 App 主題包裝
            CalculatorAppTheme {
                // ✅ 使用 Scaffold 作為最外層佈局
                Scaffold(
                    bottomBar = {
                        // ✅ 將 FooterInfo 放在 bottomBar 中
                        FooterInfo()
                    }
                ) { innerPadding ->
                    // ✅ Box 用來容納導航圖，並套用 Scaffold 提供的 padding
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavGraph(
                            dao = itemDao,
                            recordDao = savedRecordDao,
                            onDeleteItem = { item ->
                                lifecycleScope.launch {
                                    itemDao.delete(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
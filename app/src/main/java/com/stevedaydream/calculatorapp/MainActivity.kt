package com.stevedaydream.calculatorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.stevedaydream.calculatorapp.data.AppDatabase
import com.stevedaydream.calculatorapp.data.Item
import com.stevedaydream.calculatorapp.navigation.AppNavGraph
import kotlinx.coroutines.launch



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val itemDao = db.itemDao()
        val savedRecordDao = db.savedRecordDao()

        setContent {
            AppNavGraph(
                dao = itemDao,
                recordDao = savedRecordDao,  // 傳給 NavGraph
                onDeleteItem = { item ->
                    lifecycleScope.launch {
                        itemDao.delete(item)
                    }
                }
            )
        }
    }

}

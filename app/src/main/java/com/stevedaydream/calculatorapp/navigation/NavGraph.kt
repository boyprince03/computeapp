package com.stevedaydream.calculatorapp.navigation
import com.stevedaydream.calculatorapp.ui.CalculatorScreen
import com.stevedaydream.calculatorapp.data.SavedRecord

import com.google.*
import java.util.Date
import com.stevedaydream.calculatorapp.ui.ExcelImportScreen
 import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stevedaydream.calculatorapp.data.AppDatabase
import com.stevedaydream.calculatorapp.data.Item
import com.stevedaydream.calculatorapp.data.ItemDao
import com.stevedaydream.calculatorapp.data.SavedRecordDao
import com.stevedaydream.calculatorapp.ui.EditScreen
import com.stevedaydream.calculatorapp.ui.MainScreen
import kotlinx.coroutines.runBlocking
import com.stevedaydream.calculatorapp.ui.HistoryScreen
import com.stevedaydream.calculatorapp.ui.SplashScreen

@Composable
fun AppNavGraph(
    dao: ItemDao,
    recordDao: SavedRecordDao, // 必須有
    onDeleteItem: (Item) -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        // 🏠 主畫面：顯示所有項目
        composable("splash") {
            SplashScreen(navController)
        }
        composable("home") {
            MainScreen(
                dao = dao,
                onAddClick = { navController.navigate("edit") },
                onEditClick = { id -> navController.navigate("edit/$id") },
                navController = navController,
                navToImport = { navController.navigate("import") }, // ✅ 加這一行
                onDeleteClick = { item -> onDeleteItem(item) },
                recordDao = recordDao,
                onHistoryClick = { navController.navigate("history") }

            )
        }

        // ➕ 新增畫面
        composable("edit") {
            EditScreen(
                item = null,
                onBack = { navController.popBackStack() },
                onSave = { item ->
                    runBlocking {
                        dao.insert(item)
                    }
                    navController.popBackStack()
                }
            )
        }
        composable("calculator") {
            CalculatorScreen(
                dao = dao,
                recordDao = recordDao,
                onManageClick = { navController.navigate("home") },
                onHistoryClick = { navController.navigate("history") } // 如果有這個參數
            )
        }

        composable("import") {
            ExcelImportScreen(
                dao = dao,
                onBack = { navController.popBackStack() }
            )
        }
        composable("history") {
            HistoryScreen(
                recordDao = recordDao,
                onBack = { navController.popBackStack() }
            )
        }

        // ✏️ 編輯畫面（帶 ID）
        composable(
            "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val item = runBlocking { dao.getById(id) }

            EditScreen(
                item = item,
                onBack = { navController.popBackStack() },
                onSave = { updatedItem ->
                    runBlocking {
                        dao.update(updatedItem)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}

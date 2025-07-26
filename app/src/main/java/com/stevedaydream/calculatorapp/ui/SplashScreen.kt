// 📄 com.stevedaydream.calculatorapp.ui.SplashScreen.kt
package com.stevedaydream.calculatorapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.navigation.NavHostController
import com.stevedaydream.calculatorapp.R

@Composable
fun SplashScreen(navController: NavHostController) {
    var scale by remember { mutableStateOf(0f) }

    // 🎬 動畫設定：從 0f 放大至 1f，彈性動畫
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = ""
    )

    // ⏳ 啟動動畫與跳轉
    LaunchedEffect(true) {
        scale = 1f
        delay(1000)
        navController.navigate("calculator") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6E0F8)), // 淡紫色背景
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo_withoutback), // 需準備 logo 圖片
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(180.dp)
                    .scale(animatedScale)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Mini Calc Web", fontSize = 22.sp, color = Color(0xFF4A148C)) // 深紫字
        }
    }
}

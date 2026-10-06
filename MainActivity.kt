package com.example.appconingresoycontrasea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.appconingresoycontrasea.ui.GameScreen
import com.example.appconingresoycontrasea.ui.LoginScreen
import com.example.appconingresoycontrasea.ui.theme.AppConIngresoYContraseñaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppConIngresoYContraseñaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("admin") }

    Crossfade(
        targetState = isLoggedIn,
        label = "ScreenTransition"
    ) { loggedIn ->
        if (loggedIn) {
            GameScreen(
                username = username,
                onLogout = {
                    isLoggedIn = false
                }
            )
        } else {
            LoginScreen(
                onLoginSuccess = { user ->
                    username = user
                    isLoggedIn = true
                }
            )
        }
    }
}

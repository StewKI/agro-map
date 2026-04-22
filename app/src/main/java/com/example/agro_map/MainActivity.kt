package com.example.agro_map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.agro_map.ui.navigation.AgroMapNavGraph
import com.example.agro_map.ui.navigation.LoginRoute
import com.example.agro_map.ui.navigation.MapRoute
import com.example.agro_map.ui.theme.AgromapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AgroMapApplication).container
        val startDestination: Any = if (container.authRepository.isLoggedIn) MapRoute else LoginRoute

        setContent {
            AgromapTheme {
                AgroMapNavGraph(
                    container = container,
                    startDestination = startDestination
                )
            }
        }
    }
}

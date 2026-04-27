package com.proyecto.cafetin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.proyecto.cafetin.navigation.NavGraph
import com.proyecto.cafetin.ui.theme.CafetinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CafetinTheme {
                NavGraph()
            }
        }
    }
}

package com.kcff.safe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kcff.safe.data.KcRepository
import com.kcff.safe.ui.KcffApp
import com.kcff.safe.ui.KcffTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = KcRepository(applicationContext)
        setContent {
            KcffTheme {
                KcffApp(repository)
            }
        }
    }
}

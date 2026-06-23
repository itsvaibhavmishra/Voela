package com.vaibhawmishra.voela

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vaibhawmishra.voela.data.audio.WorkingCache
import com.vaibhawmishra.voela.ui.navigation.VoelaNavHost
import com.vaibhawmishra.voela.ui.theme.VoelaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Drop any leftover transcode temp from a killed save
        Thread { WorkingCache.sweep(applicationContext) }.start()
        // Dark-only app — keep light icons over transparent bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            VoelaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    VoelaNavHost()
                }
            }
        }
    }
}

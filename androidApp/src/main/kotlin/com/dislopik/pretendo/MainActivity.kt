package com.dislopik.pretendo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dislopik.pretendo.data.initPretendoAndroid

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // The shared module reaches Android storage and system services through this.
        initPretendoAndroid(applicationContext)

        setContent {
            App()
        }
    }
}

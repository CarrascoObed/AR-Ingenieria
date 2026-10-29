package com.aring.movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aring.movil.data.Prefs
import com.aring.movil.data.panel.TableroDraftStore
import com.aring.movil.ui.theme.ARIngMovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(applicationContext)
        TableroDraftStore.init(applicationContext)
        setContent {
            ARIngMovilTheme {
                App()
            }
        }
    }
}

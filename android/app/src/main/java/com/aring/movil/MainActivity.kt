package com.aring.movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aring.movil.data.Prefs
import com.aring.movil.data.OfflineSync
import com.aring.movil.data.RefrescoSesion
import com.aring.movil.data.OfflineTaskCache
import com.aring.movil.data.panel.TableroDraftStore
import com.aring.movil.ui.theme.ARIngMovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(applicationContext)
        OfflineSync.initWithContext(applicationContext)
        OfflineSync.reintentarPendientes(applicationContext)
        OfflineTaskCache.init(applicationContext)
        TableroDraftStore.init(applicationContext)
        if (savedInstanceState == null) {
            TableroDraftStore.limpiarIncompletos()
        }
        RefrescoSesion.programar(applicationContext)
        setContent {
            ARIngMovilTheme {
                App()
            }
        }
    }
}

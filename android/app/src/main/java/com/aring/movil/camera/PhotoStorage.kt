package com.aring.movil.camera

import android.content.Context
import com.aring.movil.data.panel.newId
import java.io.File

object PhotoStorage {

    fun newPhotoFile(context: Context): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "foto_${System.currentTimeMillis()}_${newId().take(8)}.jpg")
    }
}

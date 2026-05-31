package com.example.pharmamapapp

import android.app.Application
import android.view.View
import com.example.pharmamapapp.appwrite.AppwriteManager
import java.util.Locale

class PharmaMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val arabicLocale = Locale("ar")
        Locale.setDefault(arabicLocale)
        val config = resources.configuration
        config.setLocale(arabicLocale)
        config.setLayoutDirection(arabicLocale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        AppwriteManager.init(this)
    }
}

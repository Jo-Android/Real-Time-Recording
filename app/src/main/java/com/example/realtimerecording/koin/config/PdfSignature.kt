package com.example.realtimerecording.koin.config

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.example.realtimerecording.koin.module.Modules.manager
import com.example.realtimerecording.koin.module.Modules.viewModel
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class PdfSignature : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalContext.startKoin {
            androidLogger(Level.DEBUG)
            modules(listOf(viewModel, manager))
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
package com.lunarlog

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lunarlog.data.AppDatabase
import com.lunarlog.data.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LunarLogApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer
    
    // We use a SupervisorJob so that a failure in one child doesn't cancel others
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        applicationScope.launch(Dispatchers.IO) {
            databaseInitializer.initialize()
        }
    }
}
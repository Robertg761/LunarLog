package com.lunarlog

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lunarlog.data.AppDatabase
import com.lunarlog.data.DatabaseInitializer
import com.lunarlog.ui.widget.WidgetDataObserver
import com.lunarlog.workers.NotificationWorkScheduler
import com.lunarlog.workers.WidgetRefreshWorker
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

    @Inject
    lateinit var widgetDataObserver: WidgetDataObserver

    // We use a SupervisorJob so that a failure in one child doesn't cancel others
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationWorkScheduler.scheduleMedicationReminders(this)

        // Widgets have no lifecycle of their own to hang this off: they are drawn by the launcher and
        // only rebuild when something asks them to. The observer covers data changes, the worker
        // covers the daily rollover.
        widgetDataObserver.start(this, applicationScope)
        WidgetRefreshWorker.schedule(this)

        applicationScope.launch(Dispatchers.IO) {
            databaseInitializer.initialize()
        }
    }
}

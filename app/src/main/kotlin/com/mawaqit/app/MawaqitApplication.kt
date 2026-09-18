package com.mawaqit.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * PHASE_3: implements Configuration.Provider so WorkManager uses Hilt's
 * HiltWorkerFactory (needed by AlarmWorker / MarkPrayedWorker, which get Room
 * and AlarmScheduler injected). This REQUIRES disabling WorkManager's default
 * initializer in the manifest — see the androidx.startup provider there.
 */
@HiltAndroidApp
class MawaqitApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

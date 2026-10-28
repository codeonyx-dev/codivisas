package com.codeonyx.bcvaqui.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codeonyx.bcvaqui.update.AppUpdateManager
import kotlinx.coroutines.flow.first

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val auto = com.codeonyx.bcvaqui.data.local.PreferencesRepository(applicationContext).preferences.first().autoUpdateCheck
        if (auto) AppUpdateManager.checkForUpdate(applicationContext, notifyIfAvailable = true)
        return Result.success()
    }
}

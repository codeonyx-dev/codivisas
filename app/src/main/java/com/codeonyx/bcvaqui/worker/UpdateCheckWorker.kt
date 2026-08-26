package com.codeonyx.bcvaqui.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codeonyx.bcvaqui.update.AppUpdateManager

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        AppUpdateManager.checkForUpdate(applicationContext, notifyIfAvailable = true)
        return Result.success()
    }
}

package com.aritxonly.cloudlane

import android.content.Context
import androidx.work.*
import com.aritxonly.cloudlane.data.PhotoScanner
import com.aritxonly.cloudlane.domain.PhotoRules
import kotlinx.coroutines.CancellationException
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class NightWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val prefs=applicationContext.getSharedPreferences("settings",0)
        if(!prefs.getBoolean("night",false)||!PhotoRules.isNight(LocalTime.now())) return Result.success()
        return try { PhotoScanner(applicationContext).scan();Result.success() }
        catch(e: CancellationException) { throw e }
        catch(e: SecurityException) { prefs.edit().putString("nightError","请打开 App 重新授权照片访问").apply();Result.failure() }
        catch(e: Exception) { prefs.edit().putString("nightError","夜间分析未完成，将重试").apply();Result.retry() }
    }
    companion object {
        fun configure(context: Context, enabled: Boolean) {
            context.getSharedPreferences("settings",0).edit().putBoolean("night",enabled).apply()
            val manager=WorkManager.getInstance(context)
            if(!enabled) { manager.cancelUniqueWork("night-analysis");return }
            val constraints=Constraints.Builder().setRequiresCharging(true).setRequiredNetworkType(NetworkType.UNMETERED).setRequiresStorageNotLow(true).build()
            manager.enqueueUniquePeriodicWork("night-analysis",ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<NightWorker>(1,TimeUnit.HOURS).setConstraints(constraints).build())
        }
    }
}

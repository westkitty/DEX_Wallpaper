package com.westkitty.dexwallpaper.worker

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import androidx.work.*
import com.westkitty.dexwallpaper.DexApplication
import com.westkitty.dexwallpaper.core.Destination
import com.westkitty.dexwallpaper.core.Outcome
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class RotationScheduler(private val context: Context) {
    suspend fun reconcile() {
        val app = context.applicationContext as DexApplication
        val wm = WorkManager.getInstance(context)
        for (target in listOf(Destination.HOME, Destination.LOCK)) {
            val screen = app.graph.repository.dao.screen(target.name) ?: continue
            val name = "rotation-${target.name}"
            if (!screen.enabled || screen.paused) wm.cancelUniqueWork(name)
            else {
                val request = PeriodicWorkRequestBuilder<RotationWorker>(screen.intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES)
                    .setInputData(workDataOf("destination" to target.name))
                    .setInitialDelay(screen.intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()
                wm.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
            }
        }
    }
}
class RotationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as DexApplication
        app.graph.repository.initialize()
        val destination = Destination.parse(inputData.getString("destination") ?: "HOME")
        if (destination == Destination.BOTH) return Result.failure()
        return when (app.graph.repository.next(destination, automatic = true)) {
            is Outcome.Ok -> Result.success()
            is Outcome.Error -> Result.success() // Conditions are re-evaluated next period; never retry in a wakeup loop.
        }
    }
}
class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val result = goAsync()
        val app = context.applicationContext as DexApplication
        app.graph.scope.launch { try {
            app.graph.repository.initialize()
            app.graph.scheduler.reconcile()
            try { UnlockRotationMonitor.reconcile(context, app.graph.preferences.flow.first()) }
            catch (_: RuntimeException) { /* Android may deny boot-time foreground starts. Next app open can retry. */ }
        } finally { result.finish() } }
    }
}

package com.westkitty.dexwallpaper.integration

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import androidx.work.*
import com.westkitty.dexwallpaper.DexApplication
import com.westkitty.dexwallpaper.core.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutomationController(private val context: Context) {
    fun enqueue(command: AutomationCommand) {
        val request=OneTimeWorkRequestBuilder<CommandWorker>().setInputData(workDataOf("command" to command.kind.name,
            "destination" to command.destination.name,"assetId" to command.assetId,"collectionId" to command.collectionId)).build()
        WorkManager.getInstance(context).enqueueUniqueWork("explicit-wallpaper-actions",ExistingWorkPolicy.APPEND_OR_REPLACE,request)
    }
}
class CommandWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val app=applicationContext as DexApplication
        val graph=app.graph
        graph.repository.initialize()
        val parsed=CommandPolicy.parse(inputData.getString("command"),inputData.getString("destination"),inputData.getString("assetId"),inputData.getString("collectionId"))
        if(parsed is Outcome.Error) return Result.failure()
        val c=(parsed as Outcome.Ok).value
        when(c.kind) {
            CommandKind.NEXT -> graph.repository.next(c.destination)
            CommandKind.PREVIOUS -> graph.repository.previous(c.destination)
            CommandKind.PAUSE -> graph.repository.updateScreen(c.destination) { it.copy(paused=true) }
            CommandKind.RESUME -> graph.repository.updateScreen(c.destination) { it.copy(enabled=true,paused=false) }
            CommandKind.APPLY -> graph.repository.apply(c.assetId,c.destination)
            CommandKind.SWITCH_COLLECTION -> {
                if(graph.repository.dao.allCollections().none { it.id==c.collectionId }) return Result.failure()
                graph.repository.updateScreen(c.destination) { it.copy(collectionId=c.collectionId,pending="") }
            }
        }
        graph.scheduler.reconcile()
        DexWidgetProvider.updateAll(applicationContext)
        return Result.success()
    }
}
/** Opt-in and strictly whitelisted Tasker "Send Intent" endpoint. No dynamic executable scripts. */
class ExternalCommandReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        if(intent.action!="com.westkitty.dexwallpaper.COMMAND") return
        val pending=goAsync()
        val app=context.applicationContext as DexApplication
        app.graph.scope.launch {
            try {
                val settings=app.graph.preferences.flow.first()
                if(!settings.externalCommands) return@launch
                val parsed=CommandPolicy.parse(intent.getStringExtra("command"),intent.getStringExtra("destination"),intent.getStringExtra("assetId"),intent.getStringExtra("collectionId"))
                if(parsed is Outcome.Ok) {
                    if(parsed.value.destination==Destination.LOCK && !settings.externalLockCommands) return@launch
                    if(parsed.value.destination==Destination.BOTH && !settings.externalBothCommands) return@launch
                    app.graph.automation.enqueue(parsed.value)
                }
            } finally { pending.finish() }
        }
    }
}

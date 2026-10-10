package com.westkitty.dexwallpaper.integration

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.westkitty.dexwallpaper.*
import com.westkitty.dexwallpaper.core.*
import kotlinx.coroutines.launch

/** Explicit Home-only controls. Provider is non-exported; mutation uses immutable self PendingIntents. */
class DexWidgetProvider: AppWidgetProvider() {
    override fun onUpdate(context: Context,manager: AppWidgetManager,ids: IntArray) { updateAll(context) }
    override fun onReceive(context: Context,intent: Intent) {
        super.onReceive(context,intent)
        if(intent.action !in setOf(NEXT,PAUSE,ALBUM,SELECT)) return
        val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1)
        val valid=AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context,DexWidgetProvider::class.java)).contains(id)
        if(!valid) return
        val pending=goAsync()
        val app=context.applicationContext as DexApplication
        app.graph.scope.launch {
            try {
                val screen=app.graph.repository.dao.screen("HOME") ?: return@launch
                val command=when(intent.action) {
                    NEXT -> AutomationCommand(CommandKind.NEXT)
                    PAUSE -> AutomationCommand(if(screen.paused) CommandKind.RESUME else CommandKind.PAUSE)
                    ALBUM -> {
                        val collections=app.graph.repository.dao.allCollections(); if(collections.isEmpty()) return@launch
                        AutomationCommand(CommandKind.SWITCH_COLLECTION,collectionId=collections[(collections.indexOfFirst { it.id==screen.collectionId }+1).mod(collections.size)].id)
                    }
                    else -> {
                        val assets=app.graph.repository.dao.collectionAssets(screen.collectionId).filter { !it.excluded && !it.quarantined }
                        if(assets.isEmpty()) return@launch
                        AutomationCommand(CommandKind.APPLY,assetId=assets[(assets.indexOfFirst { it.id==screen.currentAssetId }+1).mod(assets.size)].id)
                    }
                }
                app.graph.automation.enqueue(command)
            } finally { pending.finish() }
        }
    }
    companion object {
        private const val NEXT="com.westkitty.dexwallpaper.widget.NEXT"
        private const val PAUSE="com.westkitty.dexwallpaper.widget.PAUSE"
        private const val ALBUM="com.westkitty.dexwallpaper.widget.ALBUM"
        private const val SELECT="com.westkitty.dexwallpaper.widget.SELECT"
        fun updateAll(context: Context) {
            val app=context.applicationContext as DexApplication
            app.graph.scope.launch {
                val manager=AppWidgetManager.getInstance(context)
                val ids=manager.getAppWidgetIds(ComponentName(context,DexWidgetProvider::class.java))
                val screen=app.graph.repository.dao.screen("HOME")
                val collection=app.graph.repository.dao.allCollections().firstOrNull { it.id==screen?.collectionId }
                for(id in ids) {
                    val views=RemoteViews(context.packageName,R.layout.dex_widget)
                    views.setTextViewText(R.id.widget_title,"DEX / HOME ONLY")
                    views.setTextViewText(R.id.widget_collection,collection?.name ?: "Personal library")
                    views.setTextViewText(R.id.widget_pause,if(screen?.paused==true) "Resume" else "Pause")
                    listOf(R.id.widget_next to NEXT,R.id.widget_pause to PAUSE,R.id.widget_album to ALBUM,R.id.widget_select to SELECT).forEach { (view,action) ->
                        val intent=Intent(context,DexWidgetProvider::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id)
                        views.setOnClickPendingIntent(view,PendingIntent.getBroadcast(context,id*10+view,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    }
                    views.setOnClickPendingIntent(R.id.widget_title,PendingIntent.getActivity(context,id,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    manager.updateAppWidget(id,views)
                }
            }
        }
    }
}

package com.westkitty.dexwallpaper

import android.app.Application
import android.content.Context
import com.westkitty.dexwallpaper.data.*
import com.westkitty.dexwallpaper.creative.CreativeRepository
import com.westkitty.dexwallpaper.wallpaper.*
import com.westkitty.dexwallpaper.worker.RotationScheduler
import kotlinx.coroutines.*

class DexApplication : Application() {
    lateinit var graph: AppGraph
        private set
    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.scope.launch { graph.repository.initialize(); graph.scheduler.reconcile() }
    }
}
class AppGraph(val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db = DexDatabase.open(context)
    val preferences = Preferences(context)
    val pipeline = BitmapPipeline()
    val importer = AssetImporter(context, db, pipeline)
    val controller = WallpaperDestinationController(AndroidWallpaperPlatform(context))
    val repository = WallRepository(context, db, importer, pipeline, controller, preferences)
    val scheduler = RotationScheduler(context)
    val creative = CreativeRepository(context, repository)
    val backup = BackupRestoreCoordinator(context, repository)
    val automation = com.westkitty.dexwallpaper.integration.AutomationController(context)
    val profiles = ProfileRepository(context, repository)
    val vault = com.westkitty.dexwallpaper.security.CredentialVault(context)
    val sync = com.westkitty.dexwallpaper.sources.SyncCoordinator(context, repository, vault)
}

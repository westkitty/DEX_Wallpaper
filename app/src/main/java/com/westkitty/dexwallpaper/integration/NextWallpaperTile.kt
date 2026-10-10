package com.westkitty.dexwallpaper.integration

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.westkitty.dexwallpaper.DexApplication
import com.westkitty.dexwallpaper.core.*

class NextWallpaperTile: TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply { state=Tile.STATE_ACTIVE; label="DEX • Next Home"; updateTile() }
    }
    override fun onClick() {
        super.onClick()
        val action=Runnable { (application as DexApplication).graph.automation.enqueue(AutomationCommand(CommandKind.NEXT,Destination.HOME)) }
        if(isLocked) unlockAndRun(action) else action.run()
    }
}

package com.westkitty.dexwallpaper.core

/** Untrusted callers cannot supply code, paths, arbitrary component names, or implicit destinations. */
enum class CommandKind { NEXT, PREVIOUS, PAUSE, RESUME, APPLY, SWITCH_COLLECTION }
data class AutomationCommand(val kind: CommandKind, val destination: Destination = Destination.HOME, val assetId: String = "", val collectionId: String = "")
object CommandPolicy {
    fun parse(action: String?, destination: String?, assetId: String?, collectionId: String?): Outcome<AutomationCommand> {
        val kind=CommandKind.entries.firstOrNull { it.name==action } ?: return Outcome.Error("Unsupported command")
        val target=if(destination==null) Destination.HOME else Destination.entries.firstOrNull { it.name==destination } ?: return Outcome.Error("Unknown destination")
        fun valid(id: String?) = id!=null && id.matches(Regex("[A-Za-z0-9_-]{1,80}"))
        if(kind==CommandKind.APPLY && !valid(assetId)) return Outcome.Error("An existing library asset ID is required")
        if(kind==CommandKind.SWITCH_COLLECTION && !valid(collectionId)) return Outcome.Error("An existing collection ID is required")
        return Outcome.Ok(AutomationCommand(kind,target,assetId ?: "",collectionId ?: ""))
    }
}

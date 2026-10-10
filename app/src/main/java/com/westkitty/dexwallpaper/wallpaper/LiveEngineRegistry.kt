package com.westkitty.dexwallpaper.wallpaper

/** Process-local active-engine evidence. No arbitrary Home-only update when ownership is unknown/shared. */
object LiveEngineRegistry {
    private val mutable=kotlinx.coroutines.flow.MutableStateFlow<List<Int>>(emptyList())
    val flow: kotlinx.coroutines.flow.StateFlow<List<Int>> = mutable
    private val owners=mutableMapOf<String,Int>()
    @Synchronized fun report(id: String,flags: Int) { owners[id]=flags;mutable.value=owners.values.toList() }
    @Synchronized fun remove(id: String) { owners.remove(id);mutable.value=owners.values.toList() }
    @Synchronized fun flags(): List<Int> = owners.values.toList()
}

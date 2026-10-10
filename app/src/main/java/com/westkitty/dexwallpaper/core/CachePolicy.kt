package com.westkitty.dexwallpaper.core

data class CacheEntry(val key: String, val bytes: Long, val lastAccess: Long, val protected: Boolean = false)
object CachePolicy {
    /** Oldest disposable entries first; originals never enter the cache budget. */
    fun evict(entries: List<CacheEntry>, maximumBytes: Long): List<String> {
        require(maximumBytes >= 0)
        var total=entries.sumOf { it.bytes.coerceAtLeast(0) }
        val evictions=mutableListOf<String>()
        for(entry in entries.filterNot { it.protected }.sortedWith(compareBy<CacheEntry> { it.lastAccess }.thenBy { it.key })) {
            if(total<=maximumBytes) break
            evictions+=entry.key; total-=entry.bytes.coerceAtLeast(0)
        }
        return evictions
    }
}

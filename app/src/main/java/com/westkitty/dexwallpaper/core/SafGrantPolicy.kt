package com.westkitty.dexwallpaper.core

/** The app retains read grants only for SAF trees still referenced by a collection. */
data class PersistedReadGrant(val uri: String, val isReadPermission: Boolean)

object SafGrantPolicy {
    /** Read grants no longer backing a collection can be released; write-only grants are untouched. */
    fun unreferencedReadUris(grants: Iterable<PersistedReadGrant>, retainedTreeUris: Set<String>): List<String> =
        grants.asSequence()
            .filter { it.isReadPermission && it.uri !in retainedTreeUris }
            .map { it.uri }
            .distinct()
            .sorted()
            .toList()
}

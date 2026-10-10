package com.westkitty.dexwallpaper.data

/** Collection picker ordering and pin-state updates; pinning never changes membership or schedules. */
object CollectionPinPolicy {
    fun forPicker(collections: Iterable<CollectionEntity>): List<CollectionEntity> =
        collections.sortedWith(compareByDescending<CollectionEntity> { it.pinned }.thenBy { it.name }.thenBy { it.id })

    fun setPinned(collection: CollectionEntity, pinned: Boolean): CollectionEntity = collection.copy(pinned = pinned)
}

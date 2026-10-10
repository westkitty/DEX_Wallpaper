package com.westkitty.dexwallpaper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionPinPolicyTest {
    @Test fun pinnedCollectionsAppearFirstThenSortByNameAndId() {
        val collections = listOf(
            CollectionEntity("z", "Zephyr"),
            CollectionEntity("b", "Beta", pinned = true),
            CollectionEntity("a", "Alpha", pinned = true),
            CollectionEntity("plain", "Alpha"),
        )

        assertEquals(
            listOf("a", "b", "plain", "z"),
            CollectionPinPolicy.forPicker(collections).map { it.id },
        )
    }

    @Test fun changingPinStatePreservesCollectionIdentityAndFolderLink() {
        val linked = CollectionEntity("collection-id", "Synced art", "content://picker/tree/active", pinned = true)
        val unpinned = CollectionPinPolicy.setPinned(linked, false)
        val repinned = CollectionPinPolicy.setPinned(unpinned, true)

        assertFalse(unpinned.pinned)
        assertTrue(repinned.pinned)
        assertEquals(linked.id, repinned.id)
        assertEquals(linked.name, repinned.name)
        assertEquals(linked.sourceTree, repinned.sourceTree)
    }
}

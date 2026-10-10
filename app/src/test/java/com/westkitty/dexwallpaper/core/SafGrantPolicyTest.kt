package com.westkitty.dexwallpaper.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SafGrantPolicyTest {
    @Test fun retainsOnlyReadGrantsReferencedByFolderCollections() {
        val grants = listOf(
            PersistedReadGrant("content://picker/tree/active", isReadPermission = true),
            PersistedReadGrant("content://picker/document/imported", isReadPermission = true),
            PersistedReadGrant("content://picker/tree/write-only", isReadPermission = false),
        )

        assertEquals(
            listOf("content://picker/document/imported"),
            SafGrantPolicy.unreferencedReadUris(grants, setOf("content://picker/tree/active")),
        )
    }

    @Test fun staleReadGrantCandidatesAreUniqueAndDeterministic() {
        val grants = listOf(
            PersistedReadGrant("content://picker/document/b", isReadPermission = true),
            PersistedReadGrant("content://picker/document/a", isReadPermission = true),
            PersistedReadGrant("content://picker/document/b", isReadPermission = true),
        )

        assertEquals(
            listOf("content://picker/document/a", "content://picker/document/b"),
            SafGrantPolicy.unreferencedReadUris(grants, emptySet()),
        )
    }
}

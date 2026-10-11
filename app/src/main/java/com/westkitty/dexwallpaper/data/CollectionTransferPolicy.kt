package com.westkitty.dexwallpaper.data

import com.westkitty.dexwallpaper.core.Outcome

/** Membership-only operations. Moving artwork never removes its protected original or edit sidecar. */
enum class CollectionTransferMode { COPY, MOVE }

data class CollectionTransferPlan(
    val addTargetMembership: Boolean,
    val removeSourceMembership: Boolean,
) {
    val changesMemberships: Boolean get() = addTargetMembership || removeSourceMembership
}

object CollectionTransferPolicy {
    fun plan(
        assetId: String,
        sourceCollectionId: String,
        targetCollectionId: String,
        existingCollectionIds: Set<String>,
        assetExists: Boolean,
        membershipCollectionIds: Set<String>,
        mode: CollectionTransferMode,
    ): Outcome<CollectionTransferPlan> {
        if (assetId.isBlank()) return Outcome.Error("Artwork is no longer available.")
        if (sourceCollectionId.isBlank() || sourceCollectionId == targetCollectionId) {
            return Outcome.Error("Choose a different destination collection.")
        }
        if (sourceCollectionId !in existingCollectionIds) return Outcome.Error("Source collection no longer exists.")
        if (targetCollectionId !in existingCollectionIds) return Outcome.Error("Destination collection no longer exists.")
        if (!assetExists) return Outcome.Error("Artwork no longer exists in the library.")
        if (sourceCollectionId !in membershipCollectionIds) {
            return Outcome.Error("Artwork is no longer a member of the source collection.")
        }

        return Outcome.Ok(
            CollectionTransferPlan(
                addTargetMembership = targetCollectionId !in membershipCollectionIds,
                removeSourceMembership = mode == CollectionTransferMode.MOVE,
            ),
        )
    }
}

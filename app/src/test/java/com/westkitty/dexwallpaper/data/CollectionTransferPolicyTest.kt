package com.westkitty.dexwallpaper.data

import com.westkitty.dexwallpaper.core.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionTransferPolicyTest {
    private val collectionIds = setOf("source", "target", "other")

    @Test fun copyAddsDestinationAndKeepsSourceMembership() {
        val plan = plan(memberships = setOf("source"), mode = CollectionTransferMode.COPY)

        assertTrue(plan.addTargetMembership)
        assertFalse(plan.removeSourceMembership)
        assertTrue(plan.changesMemberships)
    }

    @Test fun moveAddsDestinationBeforeRemovingSourceMembership() {
        val plan = plan(memberships = setOf("source"), mode = CollectionTransferMode.MOVE)

        assertTrue(plan.addTargetMembership)
        assertTrue(plan.removeSourceMembership)
    }

    @Test fun copyToExistingDestinationIsIdempotent() {
        val plan = plan(memberships = setOf("source", "target"), mode = CollectionTransferMode.COPY)

        assertFalse(plan.addTargetMembership)
        assertFalse(plan.removeSourceMembership)
        assertFalse(plan.changesMemberships)
    }

    @Test fun movingToExistingDestinationOnlyRemovesSourceLink() {
        val plan = plan(memberships = setOf("source", "target"), mode = CollectionTransferMode.MOVE)

        assertFalse(plan.addTargetMembership)
        assertTrue(plan.removeSourceMembership)
    }

    @Test fun invalidOrStaleTransferIsRejectedWithoutAPlan() {
        assertTrue(policy(assetExists = false) is Outcome.Error)
        assertTrue(policy(memberships = setOf("other")) is Outcome.Error)
        assertTrue(policy(target = "missing") is Outcome.Error)
        assertTrue(policy(source = "missing") is Outcome.Error)
        assertTrue(policy(source = "target", target = "target") is Outcome.Error)
    }

    private fun plan(
        memberships: Set<String>,
        mode: CollectionTransferMode,
    ): CollectionTransferPlan = when (val result = policy(memberships = memberships, mode = mode)) {
        is Outcome.Ok -> result.value
        is Outcome.Error -> throw AssertionError(result.message)
    }

    private fun policy(
        source: String = "source",
        target: String = "target",
        assetExists: Boolean = true,
        memberships: Set<String> = setOf("source"),
        mode: CollectionTransferMode = CollectionTransferMode.COPY,
    ) = CollectionTransferPolicy.plan(
        assetId = "asset",
        sourceCollectionId = source,
        targetCollectionId = target,
        existingCollectionIds = collectionIds,
        assetExists = assetExists,
        membershipCollectionIds = memberships,
        mode = mode,
    )
}

package com.westkitty.dexwallpaper.core

import org.junit.Assert.*
import org.junit.Test

class ShuffleGuardTest {
    private fun selection(result: Outcome<Selection>) = (result as Outcome.Ok).value
    @Test fun shuffleWithoutReplacement() {
        val candidates = (1..100).map { Candidate("$it") }
        var state = QueueState()
        val picked = (1..100).map { selection(ShuffleGuard.next(candidates, state, RotationMode.SHUFFLE, 42)).also { state = it.queue }.assetId }
        assertEquals(100, picked.toSet().size)
        assertTrue(state.pending.isEmpty())
    }
    @Test fun fairnessAcrossCycleBoundary() {
        val pool = (1..10).map { Candidate("$it") }
        var state = QueueState()
        repeat(1000) {
            val old = state.recent.takeLast(3)
            val s = selection(ShuffleGuard.next(pool, state, RotationMode.SHUFFLE, 17, 3))
            assertFalse(old.contains(s.assetId))
            state = s.queue
        }
    }
    @Test fun persistentQueueIsDeterministic() {
        val pool = (1..10).map { Candidate("$it") }
        val first = selection(ShuffleGuard.next(pool, QueueState(), RotationMode.SHUFFLE, 71))
        val restored = QueueState(first.queue.pending.toList(), first.queue.recent.toList(), first.queue.cycle)
        assertEquals(ShuffleGuard.next(pool, first.queue, RotationMode.SHUFFLE, 71), ShuffleGuard.next(pool, restored, RotationMode.SHUFFLE, 71))
    }
    @Test fun changedCollectionDropsIneligibleQueueItems() {
        val s = selection(ShuffleGuard.next(listOf(Candidate("a"), Candidate("b", excluded = true)), QueueState(listOf("missing","b","a","a")), RotationMode.SHUFFLE, 2))
        assertEquals("a", s.assetId)
        assertTrue(s.queue.pending.isEmpty())
    }
    @Test fun favoritesAreWeightedButNeverDuplicated() {
        val pool = listOf(Candidate("a"), Candidate("b", favorite = true), Candidate("c"))
        val first = selection(ShuffleGuard.next(pool, QueueState(), RotationMode.SHUFFLE, 5))
        assertEquals("b", first.assetId)
        assertEquals(2, first.queue.pending.size)
    }
    @Test fun emptyAndExcludedPoolsFail() {
        assertTrue(ShuffleGuard.next(emptyList(), QueueState(), RotationMode.SHUFFLE, 0) is Outcome.Error)
        assertTrue(ShuffleGuard.next(listOf(Candidate("x", excluded = true)), QueueState(), RotationMode.SHUFFLE, 0) is Outcome.Error)
    }
    @Test fun sequentialWraps() {
        val pool = listOf(Candidate("a"), Candidate("b"))
        assertEquals("a", selection(ShuffleGuard.next(pool, QueueState(recent = listOf("b")), RotationMode.SEQUENTIAL, 0)).assetId)
    }
    @Test fun singleImageNeverStarves() {
        val s = selection(ShuffleGuard.next(listOf(Candidate("a")), QueueState(recent = listOf("a")), RotationMode.SHUFFLE, 0, 50))
        assertEquals("a", s.assetId)
    }
    @Test fun thousandAssetsStayBounded() {
        val pool = (1..1000).map { Candidate("$it") }
        var queue = QueueState()
        repeat(2000) { queue = selection(ShuffleGuard.next(pool, queue, RotationMode.SHUFFLE, 101)).queue; assertTrue(queue.recent.size <= 100); assertTrue(queue.pending.size < 1000) }
    }
    @Test fun eligibilityExplainsExclusion() {
        assertEquals("Manually excluded", ShuffleGuard.eligibility(Candidate("a", excluded = true), false))
        assertEquals("Favorites-only filter", ShuffleGuard.eligibility(Candidate("a"), true))
    }
}

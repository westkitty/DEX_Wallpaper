package com.westkitty.dexwallpaper.core

import kotlin.random.Random

/** Pure selection. The caller persists the returned queue ONLY after a successful destination write. */
object ShuffleGuard {
    fun next(candidates: List<Candidate>, state: QueueState, mode: RotationMode, seed: Long,
             minimumSeparation: Int = 1, favoritesOnly: Boolean = false): Outcome<Selection> {
        val eligible = candidates.filter { it.valid && !it.excluded && (!favoritesOnly || it.favorite) }.distinctBy { it.id }
        if (eligible.isEmpty()) return Outcome.Error("No eligible artwork. Add images or remove filters.")
        val ids = eligible.map { it.id }
        val recent = state.recent.filter { it in ids }.takeLast(100)
        val separation = minimumSeparation.coerceIn(0, (ids.size - 1).coerceAtLeast(0))
        val avoid = recent.takeLast(separation).toSet()
        var cycle = state.cycle
        val pending = state.pending.filter { it in ids }.distinct().toMutableList()
        val selected: String
        when (mode) {
            RotationMode.SHUFFLE -> {
                if (pending.isEmpty()) {
                    cycle++
                    // Favorites are earlier in each cycle, never duplicated in the cycle.
                    val random = Random(seed xor cycle)
                    pending += eligible.shuffled(random).sortedBy { if (it.favorite) 0 else 1 }.map { it.id }
                }
                val index = pending.indexOfFirst { it !in avoid }.let { if (it < 0) 0 else it }
                selected = pending.removeAt(index)
            }
            RotationMode.SEQUENTIAL -> {
                val previous = recent.lastOrNull()
                selected = ids[(ids.indexOf(previous) + 1).mod(ids.size)]
                pending.clear()
            }
            RotationMode.RANDOM -> {
                val pool = ids.filterNot { it in avoid }.ifEmpty { ids }
                selected = pool.random(Random(seed xor cycle xor recent.size.toLong()))
                cycle++
                pending.clear()
            }
        }
        return Outcome.Ok(Selection(selected, QueueState(pending, (recent + selected).takeLast(100), cycle),
            "${mode.name.lowercase()} · cycle $cycle · ${eligible.size} eligible · separation $separation"))
    }

    fun eligibility(candidate: Candidate, favoritesOnly: Boolean): String = when {
        !candidate.valid -> "Quarantined or missing"
        candidate.excluded -> "Manually excluded"
        favoritesOnly && !candidate.favorite -> "Favorites-only filter"
        else -> "Eligible"
    }
}

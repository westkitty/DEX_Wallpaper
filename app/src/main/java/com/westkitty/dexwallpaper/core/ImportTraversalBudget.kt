package com.westkitty.dexwallpaper.core

/**
 * Bounds work while enumerating a user-authorized document tree. Every file and directory
 * consumes one entry so folders containing only non-image files cannot evade the cap.
 * The root itself is not counted; its direct children have depth 1.
 */
class ImportTraversalBudget(
    private val maximumEntries: Int = DEFAULT_MAXIMUM_ENTRIES,
    private val maximumDepth: Int = DEFAULT_MAXIMUM_DEPTH,
) {
    init {
        require(maximumEntries > 0) { "maximumEntries must be positive" }
        require(maximumDepth >= 0) { "maximumDepth must not be negative" }
    }

    var entriesVisited: Int = 0
        private set

    /** True when at least one entry was deliberately omitted by either bound. */
    var truncated: Boolean = false
        private set

    /** True once the total-entry limit, rather than only the depth limit, is reached. */
    var entryLimitReached: Boolean = false
        private set

    /**
     * Counts a discovered child and returns whether it may be inspected or descended into.
     * Too-deep entries consume budget too, preventing a very wide deep branch from bypassing
     * the total-work limit.
     */
    fun visit(depth: Int): Boolean {
        require(depth >= 0) { "depth must not be negative" }
        if (entriesVisited >= maximumEntries) {
            truncated = true
            entryLimitReached = true
            return false
        }
        entriesVisited++
        if (depth > maximumDepth) {
            truncated = true
            return false
        }
        return true
    }

    companion object {
        const val DEFAULT_MAXIMUM_ENTRIES = 5_000
        const val DEFAULT_MAXIMUM_DEPTH = 16
    }
}

package com.westkitty.dexwallpaper.core

import org.junit.Assert.*
import org.junit.Test

class ImportTraversalBudgetTest {
    @Test fun filesAndDirectoriesShareOneEntryLimit() {
        val budget = ImportTraversalBudget(maximumEntries = 2, maximumDepth = 16)

        assertTrue(budget.visit(depth = 1)) // directory
        assertTrue(budget.visit(depth = 2)) // non-image file
        assertFalse(budget.visit(depth = 2)) // the third node is not inspected

        assertEquals(2, budget.entriesVisited)
        assertTrue(budget.truncated)
        assertTrue(budget.entryLimitReached)
    }

    @Test fun exactEntryLimitIsNotReportedTruncatedWithoutAnOmittedEntry() {
        val budget = ImportTraversalBudget(maximumEntries = 2, maximumDepth = 16)

        assertTrue(budget.visit(depth = 1))
        assertTrue(budget.visit(depth = 1))

        assertEquals(2, budget.entriesVisited)
        assertFalse(budget.truncated)
        assertFalse(budget.entryLimitReached)
    }

    @Test fun depthLimitSkipsDeepBranchButDoesNotExhaustEntryBudget() {
        val budget = ImportTraversalBudget(maximumEntries = 10, maximumDepth = 2)

        assertTrue(budget.visit(depth = 1))
        assertTrue(budget.visit(depth = 2))
        assertFalse(budget.visit(depth = 3))
        assertTrue(budget.visit(depth = 2)) // a sibling at the allowed depth is still usable

        assertEquals(4, budget.entriesVisited) // the skipped deep entry is also bounded work
        assertTrue(budget.truncated)
        assertFalse(budget.entryLimitReached)
    }

    @Test fun rejectsInvalidLimitsAndDepth() {
        assertThrows(IllegalArgumentException::class.java) { ImportTraversalBudget(maximumEntries = 0) }
        assertThrows(IllegalArgumentException::class.java) { ImportTraversalBudget(maximumDepth = -1) }
        assertThrows(IllegalArgumentException::class.java) { ImportTraversalBudget().visit(depth = -1) }
    }
}

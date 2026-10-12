package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ChronoscapeTest {
    private val context = ContextSnapshot(ZonedDateTime.parse("2026-10-10T14:00:00Z"), true, 80, false)
    @Test fun highestPriorityWinsWithReceipt() {
        val rules = listOf(ContextRule("b","Low","art",1), ContextRule("a","High","work",9))
        val decision = Chronoscape.evaluate(rules, context)
        assertEquals("a", decision.winner?.id)
        assertEquals("Matched but lost priority", decision.evaluations.last().reason)
    }
    @Test fun tieBreakIsDeterministic() {
        val rules = listOf(ContextRule("b","B","b",5), ContextRule("a","A","a",5))
        assertEquals("a", Chronoscape.evaluate(rules, context).winner?.id)
        assertEquals("a", Chronoscape.evaluate(rules.reversed(), context).winner?.id)
    }
    @Test fun overnightWindowMatches() { assertNotNull(Chronoscape.evaluate(listOf(ContextRule("n","Night","night",start = LocalTime.of(22,0), end = LocalTime.of(7,0))), context.copy(time = context.time.withHour(23))).winner) }
    @Test fun overnightRuleUsesTheDayItStarted() {
        val fridayOnly = ContextRule("fri", "Friday night", "night", start = LocalTime.of(22, 0), end = LocalTime.of(2, 0), days = setOf(DayOfWeek.FRIDAY))
        fun at(value: String) = context.copy(time = ZonedDateTime.parse(value))
        assertNotNull(Chronoscape.evaluate(listOf(fridayOnly), at("2026-10-09T23:00:00Z")).winner)
        assertNotNull(Chronoscape.evaluate(listOf(fridayOnly), at("2026-10-10T01:00:00Z")).winner)
        assertNull(Chronoscape.evaluate(listOf(fridayOnly), at("2026-10-09T01:00:00Z")).winner)
        assertNull(Chronoscape.evaluate(listOf(fridayOnly), at("2026-10-10T03:00:00Z")).winner)
    }
    @Test fun overnightConflictsAcrossDayAndWeekBoundaries() {
        val friday = ContextRule("fri", "Friday", "night", priority = 5, start = LocalTime.of(22, 0), end = LocalTime.of(2, 0), days = setOf(DayOfWeek.FRIDAY))
        val saturday = ContextRule("sat", "Saturday", "morning", priority = 5, start = LocalTime.of(1, 0), end = LocalTime.of(3, 0), days = setOf(DayOfWeek.SATURDAY))
        assertEquals(1, Chronoscape.conflicts(listOf(friday, saturday)).size)
        val sunday = friday.copy(id = "sun", days = setOf(DayOfWeek.SUNDAY))
        val monday = saturday.copy(id = "mon", days = setOf(DayOfWeek.MONDAY))
        assertEquals(1, Chronoscape.conflicts(listOf(sunday, monday)).size)
        assertTrue(Chronoscape.conflicts(listOf(friday, monday)).isEmpty())
        assertNotNull(Chronoscape.evaluate(listOf(sunday), context.copy(time=ZonedDateTime.parse("2026-10-12T01:00:00Z"))).winner)
    }
    @Test fun conditionsAndMissingSensorFailClosed() {
        val decision = Chronoscape.evaluate(listOf(ContextRule("x","Lux","x", maximumLux = 5f), ContextRule("y","Dark","y", darkTheme = true)), context)
        assertNull(decision.winner)
        assertEquals("Ambient-light reading unavailable", decision.evaluations.first().reason)
    }
    @Test fun conflictsDetectOverlapNotDisjointWindows() {
        val morning = ContextRule("a","A","a",start = LocalTime.of(8,0), end = LocalTime.of(11,0))
        val evening = ContextRule("b","B","b",start = LocalTime.of(17,0), end = LocalTime.of(20,0))
        assertTrue(Chronoscape.conflicts(listOf(morning,evening)).isEmpty())
        assertEquals(1, Chronoscape.conflicts(listOf(morning,evening.copy(start = LocalTime.of(10,0)))).size)
    }
    @Test fun simulationIsBoundedAndDoesNotChangeRules() { val rules = listOf(ContextRule("a","A","a")); assertEquals(168, Chronoscape.preview(rules,context,1000).size); assertEquals(1, rules.size) }
    @Test fun nextDailyRespectsDaysAndZone() {
        val next = ScheduleMath.nextDaily(context.time,LocalTime.of(12,0),setOf(DayOfWeek.MONDAY))
        assertEquals(ZonedDateTime.parse("2026-10-12T12:00:00Z"),next)
    }
    @Test fun fallBackSecondClockOccurrenceIsStillScheduled() {
        val zone = ZoneId.of("America/Detroit")
        val now = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, zone).withLaterOffsetAtOverlap()
        val expected = ZonedDateTime.of(2026, 11, 1, 1, 40, 0, 0, zone).withLaterOffsetAtOverlap()
        assertEquals(expected, ScheduleMath.nextDaily(now, LocalTime.of(1, 40), setOf(DayOfWeek.SUNDAY)))
        val beforeFirst = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, zone).withEarlierOffsetAtOverlap()
        val first = expected.withEarlierOffsetAtOverlap()
        assertEquals(first, ScheduleMath.nextDaily(beforeFirst, LocalTime.of(1, 40), setOf(DayOfWeek.SUNDAY)))
    }
    @Test fun springForwardMissingClockTimeResolvesToValidTime() {
        val zone = ZoneId.of("America/Detroit")
        val now = ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 3, 8, 3, 30, 0, 0, zone)
        assertEquals(expected, ScheduleMath.nextDaily(now, LocalTime.of(2, 30), setOf(DayOfWeek.SUNDAY)))
    }
    @Test fun intervalHonorsWorkManagerMinimum() { assertEquals(900_100L, ScheduleMath.nextInterval(100,100,1)) }
}

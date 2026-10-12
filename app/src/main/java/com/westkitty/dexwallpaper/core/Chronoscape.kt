package com.westkitty.dexwallpaper.core

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/** Immutable snapshot input: evaluation never reads sensors, performs I/O, or writes wallpaper. */
data class ContextSnapshot(val time: ZonedDateTime, val charging: Boolean, val batteryPercent: Int, val darkTheme: Boolean, val lux: Float? = null)
data class ContextRule(val id: String, val name: String, val collectionId: String, val priority: Int = 0, val enabled: Boolean = true,
    val start: LocalTime = LocalTime.MIN, val end: LocalTime = LocalTime.MAX, val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val charging: Boolean? = null, val darkTheme: Boolean? = null, val minimumBattery: Int = 0, val maximumLux: Float? = null)
data class RuleEvaluation(val rule: ContextRule, val matches: Boolean, val reason: String, val selected: Boolean = false)
data class RuleDecision(val winner: ContextRule?, val evaluations: List<RuleEvaluation>)
object Chronoscape {
    fun evaluate(rules: List<ContextRule>, context: ContextSnapshot): RuleDecision {
        val evaluations = rules.sortedWith(compareByDescending<ContextRule> { it.priority }.thenBy { it.id }).map { rule ->
            val time = context.time.toLocalTime()
            val overnight = rule.start > rule.end
            // An overnight window belongs to the day on which it STARTED.
            // E.g. Friday 22:00–Saturday 02:00 remains a Friday rule after midnight.
            val owningDay = if (overnight && time <= rule.end) context.time.dayOfWeek.minus(1)
                            else context.time.dayOfWeek
            val inTime = if (overnight) time >= rule.start || time <= rule.end
                         else time >= rule.start && time <= rule.end
            val reason = when {
                !rule.enabled -> "Disabled"
                owningDay !in rule.days -> "Day does not match"
                !inTime -> "Outside time window"
                rule.charging != null && rule.charging != context.charging -> "Charging condition does not match"
                rule.darkTheme != null && rule.darkTheme != context.darkTheme -> "Theme does not match"
                context.batteryPercent < rule.minimumBattery -> "Battery below threshold"
                rule.maximumLux != null && context.lux == null -> "Ambient-light reading unavailable"
                rule.maximumLux != null && context.lux!! > rule.maximumLux -> "Ambient light above threshold"
                else -> "Matched"
            }
            RuleEvaluation(rule, reason == "Matched", reason)
        }
        val winner = evaluations.firstOrNull { it.matches }?.rule
        return RuleDecision(winner, evaluations.map { if (it.rule.id == winner?.id) it.copy(selected = true, reason = "Selected: highest priority; ID breaks ties") else if (it.matches) it.copy(reason = "Matched but lost priority") else it })
    }
    fun conflicts(rules: List<ContextRule>): List<Pair<String, String>> {
        val enabled = rules.filter { it.enabled }
        return enabled.flatMapIndexed { i, a -> enabled.drop(i + 1).filter { b ->
            a.priority == b.priority && a.collectionId != b.collectionId &&
                (a.charging == null || b.charging == null || a.charging == b.charging) &&
                (a.darkTheme == null || b.darkTheme == null || a.darkTheme == b.darkTheme) &&
                weeklyOverlap(a, b)
        }.map { a.id to it.id } }
    }
    /** Weekly second ranges preserve overnight spillover, including Sunday into Monday. */
    private fun weeklyRanges(rule: ContextRule): List<IntRange> = buildList {
        val secondsPerDay = 86_400
        val start = rule.start.toSecondOfDay()
        val end = rule.end.toSecondOfDay()
        for (day in rule.days) {
            val offset = (day.value - 1) * secondsPerDay
            if (rule.start <= rule.end) add(offset + start..offset + end)
            else {
                add(offset + start..offset + secondsPerDay - 1)
                val nextOffset = (day.value % 7) * secondsPerDay
                add(nextOffset..nextOffset + end)
            }
        }
    }
    private fun weeklyOverlap(a: ContextRule, b: ContextRule): Boolean {
        val aRanges = weeklyRanges(a)
        val bRanges = weeklyRanges(b)
        return aRanges.any { x -> bRanges.any { y -> x.first <= y.last && y.first <= x.last } }
    }
    fun preview(rules: List<ContextRule>, context: ContextSnapshot, hours: Int): List<Pair<ZonedDateTime, String?>> =
        (0 until hours.coerceIn(1, 168)).map { h -> context.time.plusHours(h.toLong()).let { time -> time to evaluate(rules, context.copy(time = time)).winner?.collectionId } }
}
object ScheduleMath {
    const val MIN_PERIOD_MINUTES = 15L
    fun nextDaily(now: ZonedDateTime, time: LocalTime, days: Set<DayOfWeek>): ZonedDateTime? {
        if (days.isEmpty()) return null
        // On fall-back days the same local clock time can happen twice. If the first
        // occurrence has passed, the second is still a valid future occurrence.
        // In spring gaps atZone resolves to the first valid time after the gap.
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek !in days) continue
            val local = date.atTime(time)
            val validOffsets = now.zone.rules.getValidOffsets(local)
            val occurrences = if (validOffsets.isEmpty()) listOf(local.atZone(now.zone))
                              else validOffsets.map { ZonedDateTime.ofLocal(local, now.zone, it) }
            occurrences.sorted().firstOrNull { it.isAfter(now) }?.let { return it }
        }
        return null
    }
    fun nextInterval(lastSuccess: Long, now: Long, minutes: Long): Long =
        maxOf(lastSuccess, now) + minutes.coerceIn(MIN_PERIOD_MINUTES, 10080) * 60_000
}

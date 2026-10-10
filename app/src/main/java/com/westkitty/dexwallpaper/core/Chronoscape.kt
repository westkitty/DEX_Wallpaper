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
            val inTime = if (rule.start <= rule.end) time >= rule.start && time <= rule.end else time >= rule.start || time <= rule.end
            val reason = when {
                !rule.enabled -> "Disabled"
                context.time.dayOfWeek !in rule.days -> "Day does not match"
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
            a.priority == b.priority && a.collectionId != b.collectionId && a.days.intersect(b.days).isNotEmpty() &&
                (a.charging == null || b.charging == null || a.charging == b.charging) &&
                (a.darkTheme == null || b.darkTheme == null || a.darkTheme == b.darkTheme) &&
                timeOverlap(a, b)
        }.map { a.id to it.id } }
    }
    private fun timeOverlap(a: ContextRule, b: ContextRule): Boolean = (0..1439).any { minute ->
        val t = LocalTime.of(minute / 60, minute % 60)
        fun contains(rule: ContextRule) = if (rule.start <= rule.end) t >= rule.start && t <= rule.end else t >= rule.start || t <= rule.end
        contains(a) && contains(b)
    }
    fun preview(rules: List<ContextRule>, context: ContextSnapshot, hours: Int): List<Pair<ZonedDateTime, String?>> =
        (0 until hours.coerceIn(1, 168)).map { h -> context.time.plusHours(h.toLong()).let { time -> time to evaluate(rules, context.copy(time = time)).winner?.collectionId } }
}
object ScheduleMath {
    const val MIN_PERIOD_MINUTES = 15L
    fun nextDaily(now: ZonedDateTime, time: LocalTime, days: Set<DayOfWeek>): ZonedDateTime? {
        if (days.isEmpty()) return null
        return (0..7).map { now.toLocalDate().plusDays(it.toLong()).atTime(time).atZone(now.zone) }
            .firstOrNull { it > now && it.dayOfWeek in days }
    }
    fun nextInterval(lastSuccess: Long, now: Long, minutes: Long): Long =
        maxOf(lastSuccess, now) + minutes.coerceIn(MIN_PERIOD_MINUTES, 10080) * 60_000
}

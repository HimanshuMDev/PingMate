package com.app.pingmate.utils

import java.util.Calendar
import java.util.Locale

/**
 * Local, deterministic reminder NLP:
 * - Parses prompts like:
 *   "remind me at 2:30 pm to call mom"
 *   "remind me on Sunday at 5pm about dinner"
 *   "remind me on 5th March at 7pm to send the report"
 *   "remind me on March 7 at 5 about the meeting"
 *
 * Returns (timeMillis, note) or null if we can't confidently parse a reminder.
 */
object ReminderNlp {

    fun parse(prompt: String): Pair<Long, String>? {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase()
        if (!lower.contains("remind") && !lower.contains("reminder")) return null

        val cal = Calendar.getInstance(Locale.getDefault())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // ------------------------------
        // 1) DATE PARSING (relative + explicit)
        // ------------------------------

        var isExplicitDate = false
        var isWeekday = false

        val monthMap = mapOf(
            "january" to Calendar.JANUARY, "jan" to Calendar.JANUARY,
            "february" to Calendar.FEBRUARY, "feb" to Calendar.FEBRUARY,
            "march" to Calendar.MARCH, "mar" to Calendar.MARCH,
            "april" to Calendar.APRIL, "apr" to Calendar.APRIL,
            "may" to Calendar.MAY,
            "june" to Calendar.JUNE, "jun" to Calendar.JUNE,
            "july" to Calendar.JULY, "jul" to Calendar.JULY,
            "august" to Calendar.AUGUST, "aug" to Calendar.AUGUST,
            "september" to Calendar.SEPTEMBER, "sep" to Calendar.SEPTEMBER, "sept" to Calendar.SEPTEMBER,
            "october" to Calendar.OCTOBER, "oct" to Calendar.OCTOBER,
            "november" to Calendar.NOVEMBER, "nov" to Calendar.NOVEMBER,
            "december" to Calendar.DECEMBER, "dec" to Calendar.DECEMBER
        )

        // Examples: "on 5th March", "5 March", "on 5th of March"
        val dayMonthPattern = Regex(
            """\b(?:on\s+)?(\d{1,2})(?:st|nd|rd|th)?\s+(?:of\s+)?(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\b""",
            RegexOption.IGNORE_CASE
        )
        // Examples: "on March 5th", "March 5"
        val monthDayPattern = Regex(
            """\b(?:on\s+)?(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\s+(\d{1,2})(?:st|nd|rd|th)?\b""",
            RegexOption.IGNORE_CASE
        )

        val now = Calendar.getInstance(Locale.getDefault())

        val dayMonthMatch = dayMonthPattern.find(lower)
        val monthDayMatch = monthDayPattern.find(lower)

        if (dayMonthMatch != null || monthDayMatch != null) {
            isExplicitDate = true
            val (day, monthName) = if (dayMonthMatch != null) {
                val d = dayMonthMatch.groupValues[1].toIntOrNull() ?: return null
                val m = dayMonthMatch.groupValues[2]
                d to m
            } else {
                val d = monthDayMatch!!.groupValues[2].toIntOrNull() ?: return null
                val m = monthDayMatch.groupValues[1]
                d to m
            }
            val monthIndex = monthMap[monthName.lowercase()] ?: return null
            cal.set(Calendar.MONTH, monthIndex)
            cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
        } else {
            // Relative day: "tomorrow", "today"
            if (lower.contains("tomorrow")) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            } else if (!lower.contains("today")) {
                // Weekday-based dates: "on Sunday", "next Friday"
                val weekdayMap = mapOf(
                    "sunday" to Calendar.SUNDAY,
                    "monday" to Calendar.MONDAY,
                    "tuesday" to Calendar.TUESDAY,
                    "wednesday" to Calendar.WEDNESDAY,
                    "thursday" to Calendar.THURSDAY,
                    "friday" to Calendar.FRIDAY,
                    "saturday" to Calendar.SATURDAY
                )
                val weekdayPattern = Regex(
                    """\b(?:on\s+)?(?:(this|next)\s+)?(sunday|monday|tuesday|wednesday|thursday|friday|saturday)\b""",
                    RegexOption.IGNORE_CASE
                )
                val weekdayMatch = weekdayPattern.find(lower)
                if (weekdayMatch != null) {
                    val modifier = weekdayMatch.groupValues[1].lowercase()
                    val dayName = weekdayMatch.groupValues[2].lowercase()
                    val targetDow = weekdayMap[dayName] ?: Calendar.MONDAY
                    val currentDow = cal.get(Calendar.DAY_OF_WEEK)
                    var daysAhead = (targetDow - currentDow + 7) % 7
                    if (daysAhead == 0 && modifier == "next") {
                        daysAhead = 7
                    } else if (daysAhead == 0 && modifier.isBlank()) {
                        // "on Sunday" and today is Sunday: keep today, time logic later will roll if needed
                        daysAhead = 0
                    }
                    if (daysAhead > 0) {
                        cal.add(Calendar.DAY_OF_MONTH, daysAhead)
                    }
                    isWeekday = true
                }
            }
        }

        // ------------------------------
        // 2) TIME PARSING
        // ------------------------------

        val patterns = listOf(
            // at/for 1-12:30 am/pm
            Regex("""(?:at|for|@)\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?\b""", RegexOption.IGNORE_CASE),
            // standalone 1-12:30 am/pm
            Regex("""\b(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)\b""", RegexOption.IGNORE_CASE),
            // 24h: 14:30, 9:00
            Regex("""\b(\d{1,2}):(\d{2})\b""")
        )
        var hour: Int? = null
        var minute = 0
        for (regex in patterns) {
            val match = regex.find(lower) ?: continue
            val (hStr, mStr, amPm) = when (match.groupValues.size) {
                4 -> Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                3 -> Triple(match.groupValues[1], match.groupValues[2], "")
                else -> continue
            }
            val h = hStr.toIntOrNull() ?: continue
            val m = mStr.ifBlank { "0" }.toIntOrNull() ?: 0
            val ap = amPm.lowercase()
            when {
                ap.contains("p") -> { hour = if (h in 1..11) h + 12 else h; minute = m }
                ap.contains("a") -> { hour = if (h == 12) 0 else h; minute = m }
                ap.isBlank() && regex == patterns[2] -> { hour = h; minute = m } // 24h
                ap.isBlank() && h in 1..12 -> {
                    var tempHour = if (h == 12) 0 else h
                    val nowHour = now.get(Calendar.HOUR_OF_DAY)
                    val nowMin = now.get(Calendar.MINUTE)
                    if (tempHour < nowHour || (tempHour == nowHour && m <= nowMin)) {
                        tempHour += 12
                    }
                    hour = tempHour; minute = m
                }
                else -> { hour = h; minute = m }
            }
            if (hour != null) break
        }
        if (hour == null) {
            // Fallback: any HH:MM or H:MM
            val fallback = Regex("""(\d{1,2}):(\d{2})""").find(lower)
            if (fallback != null) {
                val h = fallback.groupValues[1].toIntOrNull() ?: return null
                minute = fallback.groupValues[2].toIntOrNull() ?: 0
                if (h in 1..12) {
                    var tempHour = if (h == 12) 0 else h
                    val nowHour = now.get(Calendar.HOUR_OF_DAY)
                    val nowMin = now.get(Calendar.MINUTE)
                    if (tempHour < nowHour || (tempHour == nowHour && minute <= nowMin)) {
                        tempHour += 12
                    }
                    hour = tempHour
                } else {
                    hour = h
                }
            } else return null
        }
        cal.set(Calendar.HOUR_OF_DAY, hour!!)
        cal.set(Calendar.MINUTE, minute)

        val nowMillis = now.timeInMillis
        if (cal.timeInMillis <= nowMillis) {
            when {
                isExplicitDate -> cal.add(Calendar.YEAR, 1)
                isWeekday -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                else -> cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // ------------------------------
        // 3) NOTE EXTRACTION
        // ------------------------------

        var note = trimmed
            // Strip common reminder lead-in phrases
            .replace(Regex("""(?i)\b(set\s+)?(a\s+)?reminder\b"""), "")
            .replace(Regex("""(?i)\b(remind\s+me\s+)(to\s+|about\s+)?"""), "")
            // Remove relative words
            .replace(Regex("""(?i)\btoday\b"""), "")
            .replace(Regex("""(?i)\btomorrow\b"""), "")
            // Remove explicit date phrases
            .replace(dayMonthPattern, "")
            .replace(monthDayPattern, "")
            // Remove weekday phrases
            .replace(Regex("""(?i)\b(on\s+)?(this|next)?\s*(sunday|monday|tuesday|wednesday|thursday|friday|saturday)\b"""), "")
            // Remove time phrases
            .replace(Regex("""(?i)(at|for|@)\s*\d{1,2}(:\d{2})?\s*(am|pm|a\.m\.|p\.m\.)?"""), "")
            .replace(Regex("""(?i)\b\d{1,2}:\d{2}\b"""), "")
            .trim()
            .take(200)

        note = note.replace(Regex("""^(to|about|that)\s+""", RegexOption.IGNORE_CASE), "").trim()

        if (note.isBlank()) {
            note = "Reminder"
        }

        return cal.timeInMillis to note.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}


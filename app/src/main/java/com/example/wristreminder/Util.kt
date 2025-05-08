package com.example.wristreminder

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

object Util {
    fun getPriorityDesc(priority:Int):String{

        return when (priority) {
            0 -> "Low"
            2 -> "Urgent"
            else -> "Normal"
        }
    }

    fun humanizeDate(dateStr: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDate.parse(dateStr, formatter)
        val today = LocalDate.now()
        val diffDays = ChronoUnit.DAYS.between(today, date).toInt()
        return when {
            diffDays == 0 -> "Today"
            diffDays == 1 -> "Tomorrow"
            diffDays == -1 -> "Yesterday"
            today.year == date.year && today.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) ->
                date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            date.isBefore(today) && today.minusWeeks(1).year == date.year && today.minusWeeks(1).get(ChronoField.ALIGNED_WEEK_OF_YEAR) == date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) ->
                "Last " + date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            date.isAfter(today) && today.plusWeeks(1).year == date.year && today.plusWeeks(1).get(ChronoField.ALIGNED_WEEK_OF_YEAR) == date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) ->
                "Next " + date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }

    fun humanizeTime(timeStr: String): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val time = LocalTime.parse(timeStr, formatter)
        val now = LocalTime.now()
        return when (val diffMinutes = ChronoUnit.MINUTES.between(now, time).toInt()) {
            0 -> "Now"
            in 1..59 -> "In ${diffMinutes}m"
            in -59..-1 -> "${-diffMinutes}m ago"
            else -> time.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }





}
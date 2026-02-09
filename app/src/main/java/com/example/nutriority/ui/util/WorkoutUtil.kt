package com.example.nutriority.ui.util

import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import kotlin.math.ceil

object WorkoutUtil {

    fun calculateTotalDuration(exercises: List<WorkoutExerciseWithDetail>, includeWarmupCooldown: Boolean): String {
        val totalSeconds = exercises.filter {
            includeWarmupCooldown || it.assignment.category.equals("Exercise", ignoreCase = true)
        }.sumOf { item ->
            val assignment = item.assignment
            val sets = assignment.sets
            val restSec = parseTimeToSeconds(assignment.rest)
            
            val workSec = if (assignment.duration.isNotBlank()) {
                parseTimeToSeconds(assignment.duration)
            } else {
                val reps = assignment.reps.split("-").last().filter { it.isDigit() }.toIntOrNull() ?: 10
                reps * 3 // Estimate 3 seconds per rep
            }
            
            (sets * workSec) + ((sets - 1).coerceAtLeast(0) * restSec)
        }

        return "${ceil(totalSeconds / 60.0).toInt()} min"
    }

    fun parseTimeToSeconds(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        val lower = timeStr.lowercase().trim()
        val value = lower.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return 0
        return when {
            lower.contains("min") || (lower.contains("m") && !lower.contains("s")) -> (value * 60).toInt()
            else -> value.toInt()
        }
    }
}

package com.example.nutriority.ui.util

import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import kotlin.math.ceil

object WorkoutUtil {

    /**
     * Calculates the total duration of a workout in minutes.
     * Refined to be more realistic for various exercise types.
     */
    fun calculateTotalDuration(exercises: List<WorkoutExerciseWithDetail>, includeWarmupCooldown: Boolean): String {
        val totalSeconds = exercises.filter {
            includeWarmupCooldown || it.assignment.category.equals("Exercise", ignoreCase = true)
        }.sumOf { item ->
            val assignment = item.assignment
            // Sanity check: limit sets to a reasonable range
            val sets = assignment.sets.coerceIn(1, 20)
            val restSec = parseTimeToSeconds(assignment.rest)
            
            val workSec = if (assignment.duration.isNotBlank() && assignment.duration != "0") {
                // If there's a specific duration (like "30s,30s"), parse it safely
                parseAverageTime(assignment.duration)
            } else {
                // If there are reps, handle ranges ("8-12") or lists ("10,10,10")
                val representativeReps = parseAverageReps(assignment.reps)
                // Estimate 5 seconds per rep to account for controlled movement and setup
                representativeReps * 5
            }
            
            // Apply (Sets * Work) + (Rest intervals between sets) to ALL categories
            // This ensures adding sets to warmups/cooldowns actually increases total time
            val exerciseTime = (sets * workSec) + ((sets - 1).coerceAtLeast(0) * restSec)
            
            // Add a 90-second transition/setup buffer per exercise to realistically account for
            // moving between equipment, water breaks, and preparation.
            (exerciseTime + 90).coerceIn(0, 3600)
        }

        val totalMinutes = ceil(totalSeconds / 60.0).toInt()
        return "${if (totalMinutes == 0 && exercises.isNotEmpty()) 1 else totalMinutes} min"
    }

    /**
     * Handles comma-separated reps like "10,12,10" or ranges like "8-12".
     * Returns a single representative number of reps for timing calculation.
     */
    private fun parseAverageReps(repsStr: String): Int {
        if (repsStr.isBlank()) return 10
        
        // 1. Handle comma separated list (take the first one as representative)
        if (repsStr.contains(",")) {
            val first = repsStr.split(",").firstOrNull()?.filter { it.isDigit() }?.toIntOrNull()
            return first?.coerceIn(1, 100) ?: 10
        }
        
        // 2. Handle range (take the max value)
        if (repsStr.contains("-")) {
            val last = repsStr.split("-").lastOrNull()?.filter { it.isDigit() }?.toIntOrNull()
            return last?.coerceIn(1, 100) ?: 10
        }
        
        // 3. Single number
        return repsStr.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 100) ?: 10
    }

    /**
     * Handles comma-separated durations like "30,30,30" or single values.
     */
    private fun parseAverageTime(durationStr: String): Int {
        if (durationStr.isBlank()) return 0
        if (durationStr.contains(",")) {
            val first = durationStr.split(",").firstOrNull() ?: ""
            return parseTimeToSeconds(first)
        }
        return parseTimeToSeconds(durationStr)
    }

    fun parseTimeToSeconds(timeStr: String): Int {
        if (timeStr.isBlank() || timeStr == "0") return 0
        val lower = timeStr.lowercase().trim()
        
        val cleanStr = when {
            lower.contains(",") -> lower.split(",").first().trim()
            lower.contains("-") -> lower.split("-").last().trim()
            else -> lower
        }
        
        if (cleanStr.contains(":")) {
            val parts = cleanStr.split(":").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            return when (parts.size) {
                2 -> (parts[0] * 60) + parts[1]
                3 -> (parts[0] * 3600) + (parts[1] * 60) + parts[2]
                else -> parts.lastOrNull() ?: 0
            }
        }

        val value = cleanStr.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return 0
        
        val seconds = when {
            cleanStr.contains("min") || (cleanStr.contains("m") && !cleanStr.contains("s")) -> (value * 60).toInt()
            else -> value.toInt()
        }
        
        return seconds.coerceIn(0, 1800)
    }
}

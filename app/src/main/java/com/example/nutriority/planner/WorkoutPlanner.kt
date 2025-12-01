package com.example.nutriority.planner

/**
 * Workout planner - creates a weekly plan based on user goal.
 */
object WorkoutPlanner {

    private val weekDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun planWorkouts(goal: String, preference: String? = null): WorkoutPlan {
        // We don't need the second tuple value here (focus) — keep signature concise and avoid unused-variable warnings
        val (sessionsPerWeek, _) = when (goal.lowercase()) {
            "lose weight" -> 5 to "Cardio & Conditioning"
            "build muscle" -> 4 to "Strength"
            "keep fit" -> 3 to "Mixed"
            else -> 3 to "Mixed"
        }

        val sessionDays = selectEvenlySpacedDays(sessionsPerWeek)

        val sessions = sessionDays.mapIndexed { idx, day ->
            val duration = when (goal.lowercase()) {
                "lose weight" -> if (idx % 2 == 0) 40 else 30
                "build muscle" -> 45
                else -> 30
            }

            val sessionFocus = if (preference.isNullOrBlank()) {
                if (goal.lowercase() == "build muscle") "Strength" else "Mixed Cardio"
            } else {
                when (preference.lowercase()) {
                    "home" -> if (goal.lowercase() == "build muscle") "Bodyweight Strength" else "Cardio Circuit"
                    "gym" -> if (goal.lowercase() == "build muscle") "Weight Training" else "Treadmill/Rowing/Cardio"
                    else -> if (goal.lowercase() == "build muscle") "Strength" else "Mixed Cardio"
                }
            }

            WorkoutSession(
                day = day,
                durationMinutes = duration,
                focus = sessionFocus,
                description = "${sessionFocus} for ${duration} minutes: a mix of exercises tailored for ${goal.lowercase()}"
            )
        }

        // Estimate weekly calories burned roughly: 6-10 kcal per minute depending on intensity, use 8 as average
        val weeklyCaloriesBurn = sessions.sumOf { it.durationMinutes } * 8

        return WorkoutPlan(weeklyCaloriesBurn, sessions)
    }

    private fun selectEvenlySpacedDays(count: Int): List<String> {
        if (count <= 0) return emptyList()
        if (count >= 7) return weekDays

        val spacing = 7.0 / count
        val result = mutableListOf<String>()
        var i = 0.0
        for (n in 0 until count) {
            val idx = (i).toInt() % 7
            result.add(weekDays[idx])
            i += spacing
        }
        return result
    }
}

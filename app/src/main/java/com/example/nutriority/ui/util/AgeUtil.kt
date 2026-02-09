package com.example.nutriority.ui.util

import java.util.Calendar

object AgeUtil {
    fun calculateAge(birthDateMillis: Long?): Int {
        if (birthDateMillis == null) return 30 // Default fallback
        
        val dob = Calendar.getInstance().apply { timeInMillis = birthDateMillis }
        val today = Calendar.getInstance()
        
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }
}

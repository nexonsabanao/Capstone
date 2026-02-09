package com.example.nutriority.ui.util

import com.example.nutriority.R

object ImageUtil {

    fun getWorkoutImageResource(target: String, name: String, difficulty: String): Int {
        val searchString = ("$target $name").lowercase()
        val diff = difficulty.lowercase().trim()

        return when {
            searchString.contains("abs") || searchString.contains("core") -> when {
                diff.contains("begin") -> R.drawable.img_abs_beginner
                diff.contains("advanced") -> R.drawable.img_abs_advanced
                else -> R.drawable.img_abs_inter
            }
            searchString.contains("arm") || searchString.contains("bicep") || searchString.contains("tricep") -> when {
                diff.contains("begin") -> R.drawable.img_arm_beginner
                diff.contains("advanced") -> R.drawable.img_arm_advanced
                else -> R.drawable.img_arm_inter
            }
            searchString.contains("leg") || searchString.contains("squat") || searchString.contains("lunge") -> when {
                diff.contains("begin") -> R.drawable.img_leg_beginner
                diff.contains("advanced") -> R.drawable.img_leg_advanced
                else -> R.drawable.img_leg_inter
            }
            searchString.contains("back") -> when {
                diff.contains("begin") -> R.drawable.img_back_begginer
                diff.contains("advanced") -> R.drawable.img_back_advanced
                else -> R.drawable.img_back_inter
            }
            searchString.contains("chest") || searchString.contains("push") -> when {
                diff.contains("begin") -> R.drawable.img_chest_beginner
                diff.contains("advanced") -> R.drawable.img_chest_advanced
                else -> R.drawable.img_chest_inter
            }
            searchString.contains("shoulder") || searchString.contains("pike") -> when {
                diff.contains("begin") -> R.drawable.img_shoulder_beginner
                diff.contains("advanced") -> R.drawable.img_shoulder_advanced
                else -> R.drawable.img_shoulder_inter
            }
            searchString.contains("full body") || searchString.contains("hiit") || searchString.contains("burpee") -> when {
                diff.contains("begin") -> R.drawable.img_fullbody_beginner
                diff.contains("advanced") -> R.drawable.img_fullbody_advanced
                else -> R.drawable.img_fullbody_inter
            }
            searchString.contains("upper body") -> when {
                diff.contains("begin") -> R.drawable.img_upperbody_beginner
                diff.contains("advanced") -> R.drawable.img_upperbody_advanced
                else -> R.drawable.img_upperbody_inter
            }
            else -> when {
                diff.contains("begin") -> R.drawable.img_otherexercise_beginner
                diff.contains("advanced") -> R.drawable.img_otherexercise_advanced
                else -> R.drawable.img_otherexercise_inter
            }
        }
    }
}

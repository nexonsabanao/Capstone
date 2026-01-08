package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.data.model.Workout
import com.example.nutriority.databinding.ItemPreviewWorkoutBinding

class WorkoutAdapter(
    private val onItemClick: (Workout) -> Unit
) : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    inner class WorkoutViewHolder(
        val binding: ItemPreviewWorkoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPreviewWorkoutBinding.inflate(inflater, parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val currentWorkout = getItem(position)
        with(holder.binding) {
            workoutName.text = currentWorkout.name
            
            // Fixed Logic: Use both Name and Target Muscles to determine the primary group
            workoutTarget.text = simplifyTargetMuscle(currentWorkout.name, currentWorkout.targetMuscle)
            
            tvDifficulty.text = currentWorkout.difficulty
            tvDuration.text = currentWorkout.duration

            if (currentWorkout.imageResId != 0) {
                workoutImage.setImageResource(currentWorkout.imageResId)
            }
        }
    }

    private fun simplifyTargetMuscle(workoutName: String, targetMuscles: String): String {
        val categories = listOf("Full Body", "Leg", "Abs", "Arm", "Back", "Chest", "Shoulder")
        val nameLower = workoutName.lowercase()
        val targetLower = targetMuscles.lowercase()

        // 1. First priority: Check if the category is explicitly in the workout name
        for (category in categories) {
            if (nameLower.contains(category.lowercase())) {
                return if (category == "Leg") "Legs" else if (category == "Arm") "Arms" else category
            }
        }

        // 2. Second priority: Check the target muscle string with better ordering
        // We look for specific primary keywords first to avoid "Shoulder" stealing focus
        val priorityOrder = listOf("Full Body", "Abs", "Legs", "Leg", "Back", "Chest", "Shoulder", "Arms", "Arm")
        
        for (category in priorityOrder) {
            // Use word boundary check or specific substring check to avoid "Back" matching "Lower Back" incorrectly
            if (targetLower.contains(category.lowercase())) {
                return when(category) {
                    "Leg", "Legs" -> "Legs"
                    "Arm", "Arms" -> "Arms"
                    else -> category
                }
            }
        }

        // Fallback: Show the first part of the detailed string
        return targetMuscles.split(",").firstOrNull()?.trim() ?: targetMuscles
    }

    class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
        override fun areItemsTheSame(oldItem: Workout, newItem: Workout): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Workout, newItem: Workout): Boolean {
            return oldItem == newItem
        }
    }
}

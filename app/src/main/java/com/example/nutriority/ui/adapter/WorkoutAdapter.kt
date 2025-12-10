package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.data.model.Workout
import com.example.nutriority.databinding.ItemPreviewWorkoutBinding

class WorkoutAdapter : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    class WorkoutViewHolder(val binding: ItemPreviewWorkoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPreviewWorkoutBinding.inflate(inflater, parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val currentWorkout = getItem(position)
        holder.binding.apply {
            workoutName.text = currentWorkout.name
            workoutTarget.text = currentWorkout.targetMuscle
            tvDifficulty.text = currentWorkout.difficulty
            tvDuration.text = currentWorkout.duration

            // The adapter now relies on imageResId being pre-calculated for better performance.
            if (currentWorkout.imageResId != 0) {
                workoutImage.setImageResource(currentWorkout.imageResId)
            }
        }
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

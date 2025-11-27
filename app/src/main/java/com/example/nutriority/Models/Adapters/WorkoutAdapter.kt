package com.example.nutriority.Models.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
// 1. IMPORT ListAdapter and DiffUtil for the new implementation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.Models.Workout
import com.example.nutriority.databinding.ItemWorkoutPreviewBinding

// 2. CHANGE RecyclerView.Adapter to ListAdapter.
//    - It no longer needs the list in the constructor.
//    - It requires a DiffUtil.ItemCallback.
class WorkoutAdapter : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    /**
     * Inner class to hold the views for each item in the RecyclerView.
     * Uses ItemWorkoutBinding for type-safe view access.
     */
    inner class WorkoutViewHolder(val binding: ItemWorkoutPreviewBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Called by the RecyclerView to create a new ViewHolder.
     * It inflates the item_workout_preview.xml layout.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemWorkoutPreviewBinding.inflate(inflater, parent, false)
        return WorkoutViewHolder(binding)
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     * It binds the workout data to the views in the ViewHolder.
     */
    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        // 3. USE getItem(position), a built-in method from ListAdapter.
        val currentWorkout = getItem(position)
        holder.binding.apply {
            workoutName.text = currentWorkout.name
            workoutTarget.text = currentWorkout.targetMuscle
            workoutImage.setImageResource(currentWorkout.imageResId)
        }
    }

    // 4. REMOVE getItemCount() and updateData().
    // ListAdapter handles these functions internally and more efficiently.

    // 5. ADD the required DiffUtil.ItemCallback class.
    // This tells the ListAdapter how to efficiently calculate changes in the list.
    class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
        override fun areItemsTheSame(oldItem: Workout, newItem: Workout): Boolean {
            // Check if the items represent the same object (e.g., by their unique ID).
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Workout, newItem: Workout): Boolean {
            // Check if the data within the items is the same.
            // The data class `==` operator handles this perfectly by comparing all properties.
            return oldItem == newItem
        }
    }
}

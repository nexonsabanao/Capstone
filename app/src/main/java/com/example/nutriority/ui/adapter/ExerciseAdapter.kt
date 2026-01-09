package com.example.nutriority.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.databinding.ItemExerciseBinding
import com.example.nutriority.databinding.ItemWorkoutDividerBinding
import com.example.nutriority.ui.workout.ItemMoveCallbackListener
import java.util.Collections

sealed class WorkoutItem {
    data class ExerciseItem(val exercise: Exercise) : WorkoutItem()
    data class DividerItem(val title: String) : WorkoutItem()
}

class ExerciseAdapter(
    private val onItemClick: (Exercise, Int, Int) -> Unit,
    private val onListUpdated: (List<Exercise>) -> Unit,
    private val onDragStart: (RecyclerView.ViewHolder) -> Unit,
    private val showDragHandle: Boolean = true,
    private val displayTargetMuscle: Boolean = false
) : ListAdapter<WorkoutItem, RecyclerView.ViewHolder>(WorkoutItemDiffCallback()), ItemMoveCallbackListener {

    companion object {
        private const val TYPE_EXERCISE = 0
        private const val TYPE_DIVIDER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is WorkoutItem.ExerciseItem -> TYPE_EXERCISE
            is WorkoutItem.DividerItem -> TYPE_DIVIDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_EXERCISE) {
            ExerciseViewHolder(ItemExerciseBinding.inflate(inflater, parent, false))
        } else {
            DividerViewHolder(ItemWorkoutDividerBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is ExerciseViewHolder && item is WorkoutItem.ExerciseItem) {
            holder.bind(item.exercise, position)
        } else if (holder is DividerViewHolder && item is WorkoutItem.DividerItem) {
            holder.bind(item.title)
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        val mutableList = currentList.toMutableList()
        
        // GENIUS BOUNDARY FIX: Check if we are crossing or moving a divider
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= mutableList.size || toPosition >= mutableList.size) return false
        
        // Prevent moving a divider
        if (mutableList[fromPosition] is WorkoutItem.DividerItem) return false
        
        // Prevent jumping OVER a divider
        val start = Math.min(fromPosition, toPosition)
        val end = Math.max(fromPosition, toPosition)
        for (i in start..end) {
            if (mutableList[i] is WorkoutItem.DividerItem) return false
        }

        Collections.swap(mutableList, fromPosition, toPosition)
        submitList(mutableList)
        return true
    }

    override fun onDragDropped() {
        val updatedExercises = currentList.filterIsInstance<WorkoutItem.ExerciseItem>()
            .mapIndexed { index, item ->
                item.exercise.copy(order = index)
            }
        onListUpdated(updatedExercises)
    }

    inner class DividerViewHolder(private val binding: ItemWorkoutDividerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.dividerTitle.text = title
        }
    }

    inner class ExerciseViewHolder(val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(exercise: Exercise, position: Int) {
            binding.exerciseName.text = exercise.name
            binding.exerciseDuration.text = if (displayTargetMuscle) {
                simplifyTargetMuscle(exercise.name, exercise.targetMuscle)
            } else {
                val unit = if (exercise.sets == 1) "set" else "sets"
                "${exercise.sets} $unit"
            }
            
            if (exercise.imageResId != 0) {
                binding.exerciseImage.setImageResource(exercise.imageResId)
            }

            binding.dragHandle.visibility = if (showDragHandle) View.VISIBLE else View.GONE
            if (showDragHandle) {
                binding.dragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) onDragStart(this)
                    false
                }
            }

            binding.root.setOnClickListener {
                onItemClick(exercise, position, itemCount)
            }
        }
    }

    private fun simplifyTargetMuscle(exerciseName: String, targetMuscles: String): String {
        val categories = listOf("Full Body", "Leg", "Abs", "Arm", "Back", "Chest", "Shoulder")
        val nameLower = exerciseName.lowercase()
        val targetLower = targetMuscles.lowercase()

        for (category in categories) {
            if (nameLower.contains(category.lowercase())) {
                return when(category) {
                    "Leg" -> "Legs"
                    "Arm" -> "Arms"
                    else -> category
                }
            }
        }

        val priorityOrder = listOf("Full Body", "Abs", "Legs", "Leg", "Back", "Chest", "Shoulder", "Arms", "Arm")
        for (category in priorityOrder) {
            if (targetLower.contains(category.lowercase())) {
                return when(category) {
                    "Leg", "Legs" -> "Legs"
                    "Arm", "Arms" -> "Arms"
                    else -> category
                }
            }
        }

        return targetMuscles.split(",").firstOrNull()?.trim() ?: targetMuscles
    }

    class WorkoutItemDiffCallback : DiffUtil.ItemCallback<WorkoutItem>() {
        override fun areItemsTheSame(oldItem: WorkoutItem, newItem: WorkoutItem): Boolean {
            return if (oldItem is WorkoutItem.ExerciseItem && newItem is WorkoutItem.ExerciseItem) {
                oldItem.exercise.id == newItem.exercise.id
            } else if (oldItem is WorkoutItem.DividerItem && newItem is WorkoutItem.DividerItem) {
                oldItem.title == newItem.title
            } else false
        }

        override fun areContentsTheSame(oldItem: WorkoutItem, newItem: WorkoutItem): Boolean {
            return oldItem == newItem
        }
    }
}

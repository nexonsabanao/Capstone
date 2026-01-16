package com.example.nutriority.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.R
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.example.nutriority.databinding.ItemExerciseBinding
import com.example.nutriority.databinding.ItemWorkoutDividerBinding
import com.example.nutriority.ui.workout.ItemMoveCallbackListener
import java.util.Collections

sealed class WorkoutItem {
    data class ExerciseItem(val detail: WorkoutExerciseWithDetail) : WorkoutItem()
    data class DividerItem(val title: String) : WorkoutItem()
}

class ExerciseAdapter(
    private val onItemClick: (WorkoutExerciseWithDetail, Int, Int) -> Unit,
    private val onListUpdated: (List<WorkoutExerciseWithDetail>) -> Unit,
    private val onDragStart: (RecyclerView.ViewHolder) -> Unit,
    private val showDragHandle: Boolean = true
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
            holder.bind(item.detail, position)
        } else if (holder is DividerViewHolder && item is WorkoutItem.DividerItem) {
            holder.bind(item.title)
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        val mutableList = currentList.toMutableList()
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= mutableList.size || toPosition >= mutableList.size) return false
        if (mutableList[fromPosition] is WorkoutItem.DividerItem) return false
        
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
        val updated = currentList.filterIsInstance<WorkoutItem.ExerciseItem>()
            .mapIndexed { index, item ->
                item.detail.copy(assignment = item.detail.assignment.copy(order = index))
            }
        onListUpdated(updated)
    }

    inner class DividerViewHolder(private val binding: ItemWorkoutDividerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.dividerTitle.text = title
        }
    }

    inner class ExerciseViewHolder(val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
        fun bind(item: WorkoutExerciseWithDetail, position: Int) {
            val exercise = item.exercise
            val assignment = item.assignment

            binding.exerciseName.text = exercise.name
            
            val isTimed = assignment.category.contains("Warm-up", true) || 
                          assignment.category.contains("Cool-down", true) ||
                          assignment.duration.isNotBlank()

            if (isTimed && assignment.duration.isNotBlank()) {
                binding.exerciseDuration.text = assignment.duration
            } else {
                val unit = if (assignment.sets == 1) "set" else "sets"
                binding.exerciseDuration.text = "${assignment.sets} $unit"
            }
            
            if (exercise.imageResId != 0) {
                binding.exerciseImage.setImageResource(exercise.imageResId)
            }

            // COMPLETION DESIGN: Show checkmark and change appearance if done
            if (assignment.isCompleted) {
                binding.dragHandle.setImageResource(R.drawable.ic_check_circle)
                binding.dragHandle.setColorFilter(binding.root.context.getColor(R.color.green))
                binding.root.alpha = 0.7f
                binding.exerciseName.setTextColor(binding.root.context.getColor(R.color.green))
            } else {
                binding.dragHandle.setImageResource(R.drawable.ic_drag_handle)
                binding.dragHandle.setColorFilter(null)
                binding.root.alpha = 1.0f
                binding.exerciseName.setTextColor(Color.parseColor("#212121"))
            }

            binding.dragHandle.visibility = if (showDragHandle) View.VISIBLE else View.GONE
            if (showDragHandle && !assignment.isCompleted) {
                binding.dragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) onDragStart(this)
                    false
                }
            } else {
                binding.dragHandle.setOnTouchListener(null)
            }

            binding.root.setOnClickListener {
                onItemClick(item, position, itemCount)
            }
        }
    }

    class WorkoutItemDiffCallback : DiffUtil.ItemCallback<WorkoutItem>() {
        override fun areItemsTheSame(oldItem: WorkoutItem, newItem: WorkoutItem): Boolean {
            return if (oldItem is WorkoutItem.ExerciseItem && newItem is WorkoutItem.ExerciseItem) {
                val old = oldItem.detail.assignment
                val new = newItem.detail.assignment
                old.workoutId == new.workoutId && old.exerciseId == new.exerciseId && old.category == new.category
            } else if (oldItem is WorkoutItem.DividerItem && newItem is WorkoutItem.DividerItem) {
                oldItem.title == newItem.title
            } else false
        }

        override fun areContentsTheSame(oldItem: WorkoutItem, newItem: WorkoutItem): Boolean {
            return oldItem == newItem
        }
    }
}

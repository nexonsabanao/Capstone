package com.example.nutriority.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.databinding.ItemExerciseBinding
import com.example.nutriority.ui.workout.ItemMoveCallbackListener
import java.util.Collections

class ExerciseAdapter(
    private val onItemClick: (Exercise, Int, Int) -> Unit,
    private val onListUpdated: (List<Exercise>) -> Unit,
    private val onDragStart: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<Exercise, ExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()), ItemMoveCallbackListener {

    inner class ExerciseViewHolder(val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position), position, itemCount)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemExerciseBinding.inflate(inflater, parent, false)
        return ExerciseViewHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val currentExercise = getItem(position)
        holder.binding.apply {
            exerciseName.text = currentExercise.name

            exerciseDuration.text = "${currentExercise.sets} sets"

            if (currentExercise.imageResId != 0) {
                exerciseImage.setImageResource(currentExercise.imageResId)
            }

            dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onDragStart(holder)
                }
                false
            }
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        val mutableList = currentList.toMutableList()
        Collections.swap(mutableList, fromPosition, toPosition)

        // Create a new list with updated order values
        val updatedList = mutableList.mapIndexed { index, exercise ->
            exercise.copy(order = index)
        }

        onListUpdated(updatedList)
        submitList(updatedList)
    }


    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem == newItem
        }
    }
}
package com.example.nutriority.ui.workout

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.R
import com.example.nutriority.data.model.ExerciseSet
import com.example.nutriority.databinding.ItemExerciseSetBinding

class ExerciseSetAdapter(
    private val onRepClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<ExerciseSet, ExerciseSetAdapter.ExerciseSetViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSetViewHolder {
        val binding = ItemExerciseSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExerciseSetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseSetViewHolder, position: Int) {
        val set = getItem(position)
        holder.bind(set, onRepClick, onDeleteClick, itemCount)
    }

    inner class ExerciseSetViewHolder(private val binding: ItemExerciseSetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(set: ExerciseSet, onRepClick: (Int) -> Unit, onDeleteClick: (Int) -> Unit, itemCount: Int) {
            binding.setNumber.text = set.setNumber.toString()
            binding.repsCount.text = set.reps.toString()

            val context = itemView.context
            val isDeletable = itemCount > 1

            // This logic is now correct: enabled state depends only on the number of items.
            binding.deleteButton.isEnabled = isDeletable
            binding.deleteButton.alpha = if (isDeletable) 1.0f else 0.5f

            if (set.isActive) {
                // Active state styling
                binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_active)
                binding.repsCount.setTextColor(Color.BLACK)
            } else {
                // Inactive state styling
                binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_inactive)
                binding.repsCount.setTextColor(Color.parseColor("#BDBDBD"))
            }

            binding.repsContainer.setOnClickListener {
                onRepClick(absoluteAdapterPosition)
            }

            binding.deleteButton.setOnClickListener {
                if (isDeletable) {
                    onDeleteClick(absoluteAdapterPosition)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ExerciseSet>() {
            override fun areItemsTheSame(oldItem: ExerciseSet, newItem: ExerciseSet): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ExerciseSet, newItem: ExerciseSet): Boolean {
                return oldItem == newItem
            }
        }
    }
}

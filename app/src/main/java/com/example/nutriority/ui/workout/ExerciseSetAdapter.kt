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
        holder.bind(set, position, onRepClick, onDeleteClick)
    }

    class ExerciseSetViewHolder(private val binding: ItemExerciseSetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(set: ExerciseSet, position: Int, onRepClick: (Int) -> Unit, onDeleteClick: (Int) -> Unit) {
            binding.setNumber.text = (position + 1).toString()
            binding.repsCount.text = set.reps.toString()

            val context = itemView.context
            if (set.isActive) {
                // Active state styling
                binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_active)
                binding.repsCount.setTextColor(Color.BLACK)
                binding.deleteButton.alpha = 1.0f
            } else {
                // Inactive state styling
                binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_inactive)
                binding.repsCount.setTextColor(Color.parseColor("#BDBDBD"))
                binding.deleteButton.alpha = 0.5f
            }

            binding.repsContainer.setOnClickListener {
                onRepClick(position)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ExerciseSet>() {
            override fun areItemsTheSame(oldItem: ExerciseSet, newItem: ExerciseSet): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ExerciseSet, newItem: ExerciseSet): Boolean {
                return oldItem == newItem
            }
        }
    }
}

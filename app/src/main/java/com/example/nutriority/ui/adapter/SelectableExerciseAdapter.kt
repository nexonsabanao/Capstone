package com.example.nutriority.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutriority.R
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.databinding.ItemSelectableExerciseBinding

class SelectableExerciseAdapter(
    private val onExerciseSelected: (Exercise, Boolean) -> Unit
) : RecyclerView.Adapter<SelectableExerciseAdapter.ViewHolder>() {

    private var allExercises = listOf<Exercise>()
    private var selectedKeys = mutableSetOf<String>() 
    private var currentFilter = "Exercise"

    private var displayList = listOf<Exercise>()

    private fun getExerciseKey(exercise: Exercise) = "${exercise.name}_${exercise.category}"

    fun setData(exercises: List<Exercise>, initialSelectedExercises: List<Exercise>) {
        allExercises = exercises
        selectedKeys = initialSelectedExercises.map { getExerciseKey(it) }.toMutableSet()
        updateDisplayList()
    }

    fun setFilter(category: String) {
        currentFilter = when (category) {
            "Warm-up" -> "Warm-up"
            "Cool-down" -> "Cool-down"
            else -> "Exercise"
        }
        updateDisplayList()
    }

    fun getSelectedExercises(): List<Exercise> {
        return allExercises.filter { getExerciseKey(it) in selectedKeys }
    }

    private fun updateDisplayList() {
        val filtered = allExercises.filter { exercise ->
            when (currentFilter) {
                "Exercise" -> {
                    !exercise.category.equals("Warm-up", ignoreCase = true) && 
                    !exercise.category.equals("Cool-down", ignoreCase = true)
                }
                else -> {
                    exercise.category.equals(currentFilter, ignoreCase = true)
                }
            }
        }
        
        displayList = filtered.sortedWith(compareByDescending<Exercise> { getExerciseKey(it) in selectedKeys }
            .thenBy { it.name })
            
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectableExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = displayList[position]
        holder.bind(exercise, selectedKeys.contains(getExerciseKey(exercise)))
    }

    override fun getItemCount() = displayList.size

    inner class ViewHolder(private val binding: ItemSelectableExerciseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(exercise: Exercise, isSelected: Boolean) {
            binding.tvExerciseName.text = exercise.name
            binding.tvTargetMuscle.text = exercise.targetMuscle
            binding.rbSelect.isChecked = isSelected
            
            // Use Glide for efficient loading in the Edit Workout dialog
            if (exercise.imageResId != 0) {
                Glide.with(binding.ivExerciseImage.context)
                    .load(exercise.imageResId)
                    .centerCrop()
                    .placeholder(R.drawable.img_balanced_diet)
                    .into(binding.ivExerciseImage)
            } else {
                binding.ivExerciseImage.setImageResource(R.drawable.img_balanced_diet)
            }

            binding.root.setOnClickListener {
                val key = getExerciseKey(exercise)
                val currentlySelected = selectedKeys.contains(key)
                if (currentlySelected) {
                    selectedKeys.remove(key)
                } else {
                    selectedKeys.add(key)
                }
                onExerciseSelected(exercise, !currentlySelected)
                updateDisplayList()
            }
        }
    }
}

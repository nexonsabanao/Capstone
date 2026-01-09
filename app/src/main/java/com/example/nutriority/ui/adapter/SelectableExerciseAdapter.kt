package com.example.nutriority.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.databinding.ItemSelectableExerciseBinding

class SelectableExerciseAdapter(
    private val onExerciseSelected: (Exercise, Boolean) -> Unit
) : RecyclerView.Adapter<SelectableExerciseAdapter.ViewHolder>() {

    private var allExercises = listOf<Exercise>()
    // GENIUS FIX: Track selection by NAME and CATEGORY to handle duplicate library entries
    private var selectedKeys = mutableSetOf<String>() 
    private var currentFilter = "Exercise"

    private var displayList = listOf<Exercise>()

    private fun getExerciseKey(exercise: Exercise) = "${exercise.name}_${exercise.category}"

    fun setData(exercises: List<Exercise>, initialSelectedExercises: List<Exercise>) {
        allExercises = exercises
        selectedKeys = initialSelectedExercises.map { getExerciseKey(it) }.toMutableSet()
        Log.d("SelectableAdapter", "Data set: ${exercises.size} templates, ${selectedKeys.size} selected names.")
        updateDisplayList()
    }

    fun setFilter(category: String) {
        currentFilter = when (category) {
            "Warm-up" -> "Warm-up"
            "Cool-down" -> "Cool-down"
            else -> "Exercise"
        }
        Log.d("SelectableAdapter", "Filter set to: $currentFilter")
        updateDisplayList()
    }

    fun getSelectedExercises(): List<Exercise> {
        // Return one template for each selected name/category key
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
        
        // Sorting: Selected items always at the top
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
            
            if (exercise.imageResId != 0) {
                binding.ivExerciseImage.setImageResource(exercise.imageResId)
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

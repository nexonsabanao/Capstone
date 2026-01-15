package com.example.nutriority.ui.adapter

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
    private var selectedExercises = mutableListOf<Exercise>() 
    private var currentFilter = "Exercise"
    private var displayList = listOf<Exercise>()

    fun setData(exercises: List<Exercise>, initialSelected: List<Exercise>) {
        allExercises = exercises
        selectedExercises = initialSelected.toMutableList()
        updateDisplayList()
    }

    fun setFilter(category: String) {
        currentFilter = category
        updateDisplayList()
    }

    fun getSelectedExercises(): List<Exercise> {
        return selectedExercises
    }

    private fun updateDisplayList() {
        val filtered = allExercises.filter { exercise ->
            // If the exercise is already selected in THIS category, we show it
            // Or if it's available in the library for this category
            val isMatch = when (currentFilter) {
                "Exercise" -> !exercise.category.contains("Warm-up", true) && !exercise.category.contains("Cool-down", true)
                else -> exercise.category.equals(currentFilter, true)
            }
            isMatch
        }

        displayList = filtered.sortedWith(
            compareByDescending<Exercise> { isSelected(it) }
            .thenBy { it.name }
        )
        notifyDataSetChanged()
    }

    private fun isSelected(exercise: Exercise): Boolean {
        return selectedExercises.any { it.id == exercise.id && it.category.equals(currentFilter, true) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectableExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = displayList[position]
        holder.bind(exercise, isSelected(exercise))
    }

    override fun getItemCount() = displayList.size

    inner class ViewHolder(private val binding: ItemSelectableExerciseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(exercise: Exercise, isSelected: Boolean) {
            binding.tvExerciseName.text = exercise.name
            binding.tvTargetMuscle.text = exercise.targetMuscle
            binding.rbSelect.isChecked = isSelected
            
            if (exercise.imageResId != 0) {
                Glide.with(binding.ivExerciseImage.context)
                    .load(exercise.imageResId)
                    .centerCrop()
                    .placeholder(R.drawable.img_balanced_diet)
                    .into(binding.ivExerciseImage)
            }

            binding.root.setOnClickListener {
                if (isSelected) {
                    selectedExercises.removeAll { it.id == exercise.id && it.category.equals(currentFilter, true) }
                } else {
                    selectedExercises.add(exercise.copy(category = currentFilter))
                }
                onExerciseSelected(exercise, !isSelected)
                updateDisplayList()
            }
        }
    }
}

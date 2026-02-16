package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nutriority.R
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.databinding.ItemSelectableExerciseBinding

class SelectableExerciseAdapter(
    private val onExerciseSelected: (Exercise, Boolean) -> Unit
) : RecyclerView.Adapter<SelectableExerciseAdapter.ViewHolder>() {

    private var allExercises = listOf<Exercise>()
    private var selectedExercises = mutableListOf<Exercise>() 
    private var currentCategory = "Exercise"
    private var currentTarget: String? = null
    private var currentSearchQuery: String = ""
    private var displayList = listOf<Exercise>()

    fun setData(exercises: List<Exercise>, initialSelected: List<Exercise>) {
        allExercises = exercises
        selectedExercises = initialSelected.toMutableList()
        updateDisplayList()
    }

    fun setFilter(category: String, target: String?) {
        currentCategory = category
        currentTarget = target
        updateDisplayList()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query.lowercase().trim()
        updateDisplayList()
    }

    fun getSelectedExercises(): List<Exercise> {
        return selectedExercises
    }

    private fun updateDisplayList() {
        val filtered = allExercises.filter { exercise ->
            // Category filtering
            val categoryMatch = when (currentCategory.lowercase()) {
                "warmup" -> exercise.category.lowercase().contains("warmup")
                "cooldown" -> exercise.category.lowercase().contains("cooldown")
                else -> !exercise.category.lowercase().contains("warmup") && 
                        !exercise.category.lowercase().contains("cooldown")
            }
            
            // Target muscle filtering
            val targetMatch = if (currentTarget == null) true 
                             else exercise.target.contains(currentTarget!!, ignoreCase = true)

            // Search filtering
            val searchMatch = if (currentSearchQuery.isEmpty()) true
                             else exercise.name.lowercase().contains(currentSearchQuery) || 
                                  exercise.target.lowercase().contains(currentSearchQuery)
            
            categoryMatch && targetMatch && searchMatch
        }

        displayList = filtered.sortedWith(
            compareByDescending<Exercise> { isSelected(it) }
            .thenBy { it.name }
        )
        notifyDataSetChanged()
    }

    private fun isSelected(exercise: Exercise): Boolean {
        return selectedExercises.any { it.id == exercise.id && it.category.equals(currentCategory, true) }
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
            binding.tvTargetMuscle.text = exercise.target
            binding.rbSelect.isChecked = isSelected
            
            Glide.with(binding.ivExerciseImage.context)
                .load(exercise.gifUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.ivExerciseImage)

            binding.root.setOnClickListener {
                if (isSelected) {
                    selectedExercises.removeAll { it.id == exercise.id && it.category.equals(currentCategory, true) }
                } else {
                    selectedExercises.add(exercise.copy(category = currentCategory))
                }
                onExerciseSelected(exercise, !isSelected)
                updateDisplayList()
            }
        }
    }
}

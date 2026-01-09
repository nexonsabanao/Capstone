package com.example.nutriority.ui.workout

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.R
import com.example.nutriority.data.model.ExerciseSet
import com.example.nutriority.databinding.ItemExerciseSetBinding

class ExerciseSetAdapter(
    private val onRepClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ExerciseSetAdapter.ExerciseSetViewHolder>() {

    private var sets: List<ExerciseSet> = emptyList()

    fun submitList(newList: List<ExerciseSet>) {
        val oldList = sets
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size
            
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        })
        
        sets = newList
        diffResult.dispatchUpdatesTo(this)
        
        // BUG FIX: When moving between 1 and 2 items, we must re-bind the first item
        // to enable/disable the delete button correctly while keeping animations.
        if (oldList.size <= 2 || newList.size <= 2) {
            notifyItemRangeChanged(0, sets.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSetViewHolder {
        val binding = ItemExerciseSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExerciseSetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseSetViewHolder, position: Int) {
        val set = sets[position]
        holder.bind(set, onRepClick, onDeleteClick, sets.size)
    }

    override fun getItemCount() = sets.size

    inner class ExerciseSetViewHolder(private val binding: ItemExerciseSetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(set: ExerciseSet, onRepClick: (Int) -> Unit, onDeleteClick: (Int) -> Unit, totalSets: Int) {
            binding.setNumber.text = set.setNumber.toString()
            binding.repsCount.text = set.value.toString()
            
            binding.unitLabel.text = if (set.isDuration) "sec" else "rep"

            val context = itemView.context
            val isDeletable = totalSets > 1

            binding.deleteButton.isEnabled = isDeletable
            binding.deleteButton.alpha = if (isDeletable) 1.0f else 0.5f

            if (set.isActive) {
                binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_active)
                binding.repsCount.setTextColor(Color.BLACK)
            } else {
                binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_inactive)
                binding.repsCount.setTextColor(Color.parseColor("#BDBDBD"))
            }

            binding.repsContainer.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onRepClick(pos)
                }
            }

            binding.deleteButton.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeleteClick(pos)
                }
            }
        }
    }
}

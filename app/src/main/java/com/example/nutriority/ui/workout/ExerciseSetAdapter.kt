package com.example.nutriority.ui.workout

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
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
            binding.setNumber.text = if (set.isCompleted) "" else set.setNumber.toString()
            binding.repsCount.text = set.value.toString()
            binding.unitLabel.text = if (set.isDuration) "sec" else "rep"

            val context = itemView.context
            
            // COMPLETED STATE DESIGN
            if (set.isCompleted) {
                binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.ic_check_circle)
                binding.setNumber.backgroundTintList = ContextCompat.getColorStateList(context, R.color.green)
                binding.repsCount.setTextColor(ContextCompat.getColor(context, R.color.green))
                binding.unitLabel.setTextColor(ContextCompat.getColor(context, R.color.green))
                binding.root.alpha = 0.8f
                binding.deleteButton.visibility = View.INVISIBLE
            } else {
                binding.root.alpha = 1.0f
                binding.deleteButton.visibility = View.VISIBLE
                binding.unitLabel.setTextColor(Color.parseColor("#757575"))
                
                if (set.isActive) {
                    binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_active)
                    binding.setNumber.backgroundTintList = null
                    binding.repsCount.setTextColor(Color.BLACK)
                } else {
                    binding.setNumber.background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_inactive)
                    binding.setNumber.backgroundTintList = null
                    binding.repsCount.setTextColor(Color.parseColor("#BDBDBD"))
                }
            }

            val isDeletable = totalSets > 1 && !set.isCompleted
            binding.deleteButton.isEnabled = isDeletable
            binding.deleteButton.alpha = if (isDeletable) 1.0f else 0.3f

            binding.repsContainer.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && !set.isCompleted) {
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

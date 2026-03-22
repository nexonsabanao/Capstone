package com.example.nutriority.ui.meal

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.nutriority.R
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.LayoutMealSwapBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import android.widget.ImageView
import androidx.core.content.ContextCompat

class MealSwapBottomSheetFragment(
    private val mealType: String,
    private val options: List<Meal>,
    private val onMealSwapped: (Meal) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutMealSwapBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private var selectedMeal: Meal? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutMealSwapBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvTitle.text = "Choose your ${mealType.lowercase()}"
        
        val adapter = MealOptionAdapter(options) { meal ->
            selectedMeal = meal
            binding.btnDone.isEnabled = true
            binding.btnDone.alpha = 1.0f
        }
        
        // Initial state for Done button
        binding.btnDone.isEnabled = false
        binding.btnDone.alpha = 0.5f
        
        binding.rvMealOptions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMealOptions.adapter = adapter
        
        binding.btnDone.setOnClickListener {
            selectedMeal?.let { onMealSwapped(it) }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class MealOptionAdapter(
        private val meals: List<Meal>,
        private val onMealSelected: (Meal) -> Unit
    ) : RecyclerView.Adapter<MealOptionAdapter.ViewHolder>() {
        
        private var selectedPosition = -1

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.card_meal)
            val image: ImageView = view.findViewById(R.id.iv_meal_image)
            val name: TextView = view.findViewById(R.id.tv_meal_name)
            val time: TextView = view.findViewById(R.id.tv_meal_time)
            val overlay: ImageView = view.findViewById(R.id.iv_select_overlay)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selectable_meal, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val meal = meals[position]
            holder.name.text = meal.name
            
            // Fix: Use the actual duration from the meal model
            val duration = if (meal.duration > 0) meal.duration else 10
            holder.time.text = "$duration min"
            
            Glide.with(holder.itemView.context)
                .load(meal.imageName)
                .placeholder(R.drawable.bg_meal_placeholder)
                .into(holder.image)
            
            val isSelected = position == selectedPosition
            
            // Set icon color based on selection
            val iconColor = if (isSelected) {
                ContextCompat.getColor(holder.itemView.context, R.color.green)
            } else {
                ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
            }
            holder.overlay.imageTintList = ColorStateList.valueOf(iconColor)
            
            holder.card.strokeWidth = if (isSelected) 4 else 0
            holder.card.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.green)

            holder.itemView.setOnClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION && selectedPosition != currentPos) {
                    val previous = selectedPosition
                    selectedPosition = currentPos
                    notifyItemChanged(previous)
                    notifyItemChanged(selectedPosition)
                    onMealSelected(meal)
                }
            }
        }

        override fun getItemCount() = meals.size
    }
}

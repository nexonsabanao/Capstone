package com.example.nutriority.ui.profile

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.R
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.databinding.FragmentLogManualBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LogManualFragment : BaseBindingFragment<FragmentLogManualBinding>(FragmentLogManualBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()
    @Inject lateinit var mealRepository: MealRepository

    private val ingredientsList = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Clear previous data every time the view is created to ensure a fresh start
        clearFields()
        
        setupDropdown()
        setupClickListeners()
        updateIngredientsUi()
    }

    private fun clearFields() {
        ingredientsList.clear()
        binding.etTitle.setText("")
        binding.etCalories.setText("")
        binding.etProtein.setText("")
        binding.etCarbs.setText("")
        binding.etFats.setText("")
        binding.spinnerMealTime.setText("Breakfast", false)
        updateIngredientsUi()
    }

    private fun setupDropdown() {
        val items = listOf("Breakfast", "Lunch", "Dinner")
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, items)
        binding.spinnerMealTime.setAdapter(adapter)
        
        // Fix for the dropdown showing only the selected item after first click
        binding.spinnerMealTime.setOnClickListener {
            binding.spinnerMealTime.showDropDown()
        }
        
        // Ensure that when an item is selected, the text is set WITHOUT filtering
        binding.spinnerMealTime.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            binding.spinnerMealTime.setText(selectedItem, false)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { 
            KeyboardUtil.hideKeyboard(requireActivity())
            navigationViewModel.goBack() 
        }
        binding.btnAddIngredient.setOnClickListener { showAddIngredientDialog() }
        binding.btnLogMeal.setOnClickListener { 
            KeyboardUtil.hideKeyboard(requireActivity())
            saveMeal() 
        }
    }

    private fun showAddIngredientDialog() {
        val input = EditText(requireContext()).apply { 
            hint = "Enter ingredient name"
            setPadding(48, 48, 48, 48)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Add Ingredient")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    ingredientsList.add(name)
                    updateIngredientsUi()
                }
                KeyboardUtil.hideKeyboard(requireActivity())
            }
            .setNegativeButton("Cancel") { _, _ ->
                KeyboardUtil.hideKeyboard(requireActivity())
            }
            .show()
    }

    private fun updateIngredientsUi() {
        binding.ingredientsContainer.removeAllViews()
        binding.tvIngredientEmpty.isVisible = ingredientsList.isEmpty()

        ingredientsList.forEachIndexed { index, ingredient ->
            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 8, 0, 8)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val textView = TextView(requireContext()).apply {
                text = "• $ingredient"
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val deleteIcon = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_close)
                layoutParams = LinearLayout.LayoutParams(48, 48)
                setPadding(8, 8, 8, 8)
                imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
                setOnClickListener {
                    ingredientsList.removeAt(index)
                    updateIngredientsUi()
                }
            }

            itemLayout.addView(textView)
            itemLayout.addView(deleteIcon)
            binding.ingredientsContainer.addView(itemLayout)
        }
    }

    private fun saveMeal() {
        val title = binding.etTitle.text.toString().trim()
        val protein = binding.etProtein.text.toString().toIntOrNull() ?: 0
        val carbs = binding.etCarbs.text.toString().toIntOrNull() ?: 0
        val fats = binding.etFats.text.toString().toIntOrNull() ?: 0
        val calories = binding.etCalories.text.toString().toIntOrNull() ?: 0
        val mealTime = binding.spinnerMealTime.text.toString()

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Please add a title", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                mealRepository.logManualMeal(title, protein, carbs, fats, mealTime, ingredientsList, calories)
                Toast.makeText(requireContext(), "Meal logged successfully!", Toast.LENGTH_SHORT).show()
                navigationViewModel.goBack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to log meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

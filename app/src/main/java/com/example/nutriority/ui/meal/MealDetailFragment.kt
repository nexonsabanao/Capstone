package com.example.nutriority.ui.meal

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.FragmentMealDetailBinding
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MealDetailFragment : Fragment() {

    private var _binding: FragmentMealDetailBinding? = null
    private val binding get() = _binding!!
    private var currentMeal: Meal? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Hide title when expanded to match WorkoutDetail style
        binding.collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT)

        // Get meal data from arguments
        val mealJson = arguments?.getString("meal_json")
        if (mealJson != null) {
            currentMeal = Gson().fromJson(mealJson, Meal::class.java)
            displayMealDetails()
        }

        setupToggleGroup()
    }

    private fun displayMealDetails() {
        currentMeal?.let { meal ->
            binding.collapsingToolbar.title = meal.name
            binding.mealName.text = meal.name
            binding.mealCalories.text = "${meal.calories} kcal"
            binding.mealTime.text = meal.time ?: "30 min"
            
            if (meal.imageResId != 0) {
                binding.mealImage.setImageResource(meal.imageResId)
            }

            showInstructions()
        }
    }

    private fun setupToggleGroup() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnInstructions.id -> showInstructions()
                    binding.btnIngredients.id -> showIngredients()
                }
            }
        }
    }

    private fun showInstructions() {
        binding.sectionTitle.text = "Instructions"
    }

    private fun showIngredients() {
        binding.sectionTitle.text = "Ingredients"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.nutriority.ui.meal

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.FragmentMealDetailBinding
import com.example.nutriority.ui.NavigationViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MealDetailFragment : Fragment() {

    private var _binding: FragmentMealDetailBinding? = null
    private val binding get() = _binding!!
    
    private val navigationViewModel: NavigationViewModel by activityViewModels()
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

        // Set up toolbar back button to use our custom back logic
        binding.toolbar.setNavigationOnClickListener {
            navigationViewModel.goBack()
        }

        // Hide title when expanded
        binding.collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT)
        binding.collapsingToolbar.setCollapsedTitleTextColor(Color.BLACK)

        setupToggleGroup()
        observeMealData()
    }

    private fun observeMealData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.selectedMealJson.collect { json ->
                    if (json != null) {
                        currentMeal = Gson().fromJson(json, Meal::class.java)
                        displayMealDetails()
                        // Scroll to top when new data loaded
                        binding.nestedScrollView.scrollTo(0, 0)
                        binding.appBarLayout.setExpanded(true)
                    }
                }
            }
        }
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

            // Fix: Use checkedButtonId from the toggle group instead of isChecked on the button
            if (binding.toggleGroup.checkedButtonId == binding.btnIngredients.id) {
                showIngredients()
            } else {
                showInstructions()
            }
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
        // Update content_recycler_view with instructions
    }

    private fun showIngredients() {
        binding.sectionTitle.text = "Ingredients"
        // Update content_recycler_view with ingredients
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

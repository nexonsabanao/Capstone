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
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

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

        // Set up custom back button listener
        binding.btnBack.setOnClickListener {
            navigationViewModel.goBack()
        }

        // Initially hide the toolbar title and background
        binding.tvToolbarTitle.alpha = 0f
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)

        // Handle app bar collapse state with a smooth fade effect
        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener

            val percentage = abs(verticalOffset).toFloat() / totalScrollRange

            // Start fading in the background and title during the last 20% of scroll
            val startFadeAt = 0.8f
            if (percentage > startFadeAt) {
                // Map the 0.8 -> 1.0 range to 0.0 -> 1.0
                val alphaProgress = (percentage - startFadeAt) / (1f - startFadeAt)
                val alphaInt = (alphaProgress * 255).toInt().coerceIn(0, 255)

                // Set white background with calculated alpha
                binding.toolbar.setBackgroundColor(Color.argb(alphaInt, 255, 255, 255))

                // Fade in the title
                binding.tvToolbarTitle.alpha = alphaProgress
            } else {
                binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
                binding.tvToolbarTitle.alpha = 0f
            }
        })

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
            binding.tvToolbarTitle.text = meal.name
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

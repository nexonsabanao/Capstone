package com.example.nutriority.ui.meal

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.ActivityMealDetailBinding
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MealDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealDetailBinding
    private var currentMeal: Meal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Hide title when expanded to match WorkoutDetail style
        binding.collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT)

        // Get meal data from intent
        val mealJson = intent.getStringExtra("meal_json")
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
}

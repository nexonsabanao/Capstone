package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentFourthScreenBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint

private sealed class DietType(val value: String) {
    object Balanced : DietType("Balanced")
    object LowCarb : DietType("Low-Carb")
    object Vegetarian : DietType("Vegetarian")
}

@AndroidEntryPoint
class FourthScreen : BaseBindingFragment<FragmentFourthScreenBinding>(FragmentFourthScreenBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private var selectedDiet: DietType? = null
    private var initialValueRestored = false
    private val dietDetailsMap by lazy { createDietDetailsMap() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAndSetInitialState()
    }

    override fun onResume() {
        super.onResume()
        setupClickListeners()
    }

    override fun onPause() {
        super.onPause()
        clearClickListeners()
    }

    private fun observeAndSetInitialState() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (!initialValueRestored) {
                val previousDiet = when (user?.preferredDiet) {
                    DietType.Balanced.value -> DietType.Balanced
                    DietType.LowCarb.value -> DietType.LowCarb
                    DietType.Vegetarian.value -> DietType.Vegetarian
                    else -> null
                }

                if (previousDiet != null) {
                    handleCardSelection(previousDiet)
                } else {
                    disableNextButton()
                }
                initialValueRestored = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.balancedCard.setOnClickListener { handleCardSelection(DietType.Balanced) }
        binding.lowCarbCard.setOnClickListener { handleCardSelection(DietType.LowCarb) }
        binding.vegetarianCard.setOnClickListener { handleCardSelection(DietType.Vegetarian) }

        binding.detailsBalanced.setOnClickListener {
            val details = dietDetailsMap[DietType.Balanced] ?: return@setOnClickListener
            showDetailsDialog(details.first, details.second)
        }
        binding.detailsLowCarb.setOnClickListener {
            val details = dietDetailsMap[DietType.LowCarb] ?: return@setOnClickListener
            showDetailsDialog(details.first, details.second)
        }
        binding.detailsVegetarian.setOnClickListener {
            val details = dietDetailsMap[DietType.Vegetarian] ?: return@setOnClickListener
            showDetailsDialog(details.first, details.second)
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
        binding.nextButton.setOnClickListener {
            selectedDiet?.let { diet ->
                userViewModel.updateOnboardingData { it.copy(preferredDiet = diet.value) }
            }
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    private fun clearClickListeners() {
        with(binding) {
            balancedCard.setOnClickListener(null)
            lowCarbCard.setOnClickListener(null)
            vegetarianCard.setOnClickListener(null)
            detailsBalanced.setOnClickListener(null)
            detailsLowCarb.setOnClickListener(null)
            detailsVegetarian.setOnClickListener(null)
            backButton.setOnClickListener(null)
            nextButton.setOnClickListener(null)
        }
    }

    private fun handleCardSelection(diet: DietType) {
        selectedDiet = diet

        val context = requireContext()
        val primaryDarkColor = ContextCompat.getColor(context, R.color.primary_dark)
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(context, R.color.dark_gray)

        val uiMap = mapOf(
            DietType.Balanced to Triple(binding.balancedCard, binding.radioBalanced, binding.detailsBalanced),
            DietType.LowCarb to Triple(binding.lowCarbCard, binding.radioLowCarb, binding.detailsLowCarb),
            DietType.Vegetarian to Triple(binding.vegetarianCard, binding.radioVegetarian, binding.detailsVegetarian)
        )

        uiMap.forEach { (type, views) ->
            val isSelected = type == diet
            val (card, radio, details) = views
            card.setCardBackgroundColor(if (isSelected) primaryDarkColor else whiteColor)
            radio.isChecked = isSelected
            radio.setTextColor(if (isSelected) whiteColor else darkGrayColor)
            details.setTextColor(if (isSelected) whiteColor else primaryDarkColor)
        }

        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
    }

    private fun disableNextButton() {
        binding.nextButton.isEnabled = false
        binding.nextButton.alpha = 0.5f
    }

    private fun showDetailsDialog(title: String, message: Spanned) {
        if (parentFragmentManager.findFragmentByTag("DietDetailsDialog")?.isAdded == true) return
        DietDetailsDialogFragment.newInstance(title, message).show(parentFragmentManager, "DietDetailsDialog")
    }

    private fun String.asHtml(): Spanned = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)

    private fun createDietDetailsMap(): Map<DietType, Pair<String, Spanned>> {
        return mapOf(
            DietType.Balanced to (
                    "Balanced Diet" to """
                Aims to provide your body with all the essential nutrients it needs for optimal health by including a wide variety of foods from all major food groups.

                <b>Goals:</b>
                &#8226; Sustainable, long-term health and wellness.
                &#8226; Consistent energy levels throughout the day.
                &#8226; No strict restrictions, promoting a healthy relationship with food.

                <b>Key Components:</b>
                &#8226; <b>Lean Proteins:</b> Chicken, fish, beans, and lentils.
                &#8226; <b>Complex Carbohydrates:</b> Whole grains, oats, and brown rice.
                &#8226; <b>Healthy Fats:</b> Avocados, nuts, seeds, and olive oil.
                &#8226; <b>Vitamins & Minerals:</b> A colorful mix of fruits and vegetables.
                """.trimIndent().replace("\n", "<br>").asHtml()
                    ),
            DietType.LowCarb to (
                    "Low-Carb Diet" to """
                Focuses on limiting carbohydrates found in sugary foods, pasta, and bread. It's rich in protein, fat, and healthy vegetables.

                <b>Goals:</b>
                &#8226; Often used for weight management and fat loss.
                &#8226; Improved blood sugar control and metabolic health.
                &#8226; Increased satiety (feeling full) from protein and fats.

                <b>What to Emphasize:</b>
                &#8226; <b>Proteins:</b> Meat, poultry, fish, and eggs.
                &#8226; <b>Low-Carb Vegetables:</b> Leafy greens, broccoli, and bell peppers.
                &#8226; <b>Healthy Fats:</b> Cheese, avocado, and nuts.
                &#8226; <b>What to Limit:</b> Sugar, bread, pasta, rice, and starchy vegetables like potatoes.
                """.trimIndent().replace("\n", "<br>").asHtml()
                    ),
            DietType.Vegetarian to (
                    "Vegetarian Diet" to """
                Excludes all meat, poultry, and seafood. This plant-focused diet has several variations but is primarily based on fruits, vegetables, grains, and nuts.

                <b>Goals:</b>
                &#8226; Ethical, environmental, or health-focused eating.
                &#8226; Typically lower in saturated fat and higher in fiber.
                &#8226; Can support heart health and weight management.

                <b>Nutrient Focus:</b>
                &#8226; <b>Protein Sources:</b> Beans, lentils, tofu, tempeh, nuts, seeds, and dairy/eggs (if included).
                &#8226; <b>Iron & B12:</b> Pay close attention to getting enough iron (from spinach, lentils) and Vitamin B12 (often from fortified foods or supplements).
                """.trimIndent().replace("\n", "<br>").asHtml()
                    )
        )
    }
}

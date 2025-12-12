package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentFourthScreenBinding
import dagger.hilt.android.AndroidEntryPoint

private sealed class DietType(val value: String) {
    object Balanced : DietType("Balanced")
    object LowCarb : DietType("Low-Carb")
    object Vegetarian : DietType("Vegetarian")
}

@AndroidEntryPoint
class FourthScreen : Fragment() {

    private var _binding: FragmentFourthScreenBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private var selectedDiet: DietType? = null
    private var initialValueRestored = false

    private val dietDetailsMap by lazy { createDietDetailsMap() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFourthScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAndSetInitialState()
        // Listeners are no longer set up here.
    }

    // --- THE FIX: PART 1 ---
    // Listeners are now set up only when the fragment is fully visible and interactive.
    override fun onResume() {
        super.onResume()
        setupClickListeners()
    }

    // --- THE FIX: PART 2 ---
    // Listeners are detached when the fragment is paused. This is the key to preventing the bug.
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
            val (title, message) = dietDetailsMap[DietType.Balanced]!!
            showDetailsDialog(title, message)
        }
        binding.detailsLowCarb.setOnClickListener {
            val (title, message) = dietDetailsMap[DietType.LowCarb]!!
            showDetailsDialog(title, message)
        }
        binding.detailsVegetarian.setOnClickListener {
            val (title, message) = dietDetailsMap[DietType.Vegetarian]!!
            showDetailsDialog(title, message)
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
        binding.nextButton.setOnClickListener {
            selectedDiet?.let { diet ->
                userViewModel.updateOnboardingData { currentUserState ->
                    currentUserState.copy(preferredDiet = diet.value)
                }
            }
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    // --- THE FIX: PART 3 ---
    // A new function to nullify all listeners, preventing ghost clicks and memory leaks.
    private fun clearClickListeners() {
        binding.balancedCard.setOnClickListener(null)
        binding.lowCarbCard.setOnClickListener(null)
        binding.vegetarianCard.setOnClickListener(null)

        binding.detailsBalanced.setOnClickListener(null)
        binding.detailsLowCarb.setOnClickListener(null)
        binding.detailsVegetarian.setOnClickListener(null)

        binding.backButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
    }

    private fun handleCardSelection(diet: DietType) {
        selectedDiet = diet

        val primaryDarkColor = ContextCompat.getColor(requireContext(), R.color.primary_dark)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)

        val uiMap = mapOf(
            DietType.Balanced to Triple(binding.balancedCard, binding.radioBalanced, binding.detailsBalanced),
            DietType.LowCarb to Triple(binding.lowCarbCard, binding.radioLowCarb, binding.detailsLowCarb),
            DietType.Vegetarian to Triple(binding.vegetarianCard, binding.radioVegetarian, binding.detailsVegetarian)
        )

        uiMap.values.forEach { (card, radioButton, detailsView) ->
            card.setCardBackgroundColor(whiteColor)
            radioButton.isChecked = false
            radioButton.setTextColor(darkGrayColor)
            detailsView.setTextColor(primaryDarkColor)
        }

        uiMap[diet]?.let { (card, radioButton, detailsView) ->
            card.setCardBackgroundColor(primaryDarkColor)
            radioButton.isChecked = true
            radioButton.setTextColor(whiteColor)
            detailsView.setTextColor(whiteColor)
        }

        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
    }

    private fun disableNextButton() {
        binding.nextButton.isEnabled = false
        binding.nextButton.alpha = 0.5f
    }

    private fun showDetailsDialog(title: String, message: Spanned) {
        val dialog = DietDetailsDialogFragment.newInstance(title, message)
        dialog.show(parentFragmentManager, "DietDetailsDialog")
    }

    private fun String.asHtml(): Spanned {
        return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

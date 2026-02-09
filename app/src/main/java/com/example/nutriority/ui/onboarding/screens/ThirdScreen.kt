package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentThirdScreenBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint

private sealed class ActivityLevel(val value: String) {
    object Sedentary : ActivityLevel("Sedentary")
    object LightlyActive : ActivityLevel("Lightly Active")
    object Active : ActivityLevel("Active")
}

@AndroidEntryPoint
class ThirdScreen : BaseBindingFragment<FragmentThirdScreenBinding>(FragmentThirdScreenBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private var selectedActivityLevel: ActivityLevel? = null
    private var initialValueRestored = false

    private val defaultTextSize = 20f
    private val selectedTextSize = 24f

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
                val previousActivityLevel = when (user?.activityLevel) {
                    ActivityLevel.Sedentary.value -> ActivityLevel.Sedentary
                    ActivityLevel.LightlyActive.value -> ActivityLevel.LightlyActive
                    ActivityLevel.Active.value -> ActivityLevel.Active
                    else -> null
                }

                if (previousActivityLevel != null) {
                    selectActivityLevel(previousActivityLevel)
                } else {
                    disableNextButton()
                }
                initialValueRestored = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.sedentaryCard.setOnClickListener { selectActivityLevel(ActivityLevel.Sedentary) }
        binding.lightlyActiveCard.setOnClickListener { selectActivityLevel(ActivityLevel.LightlyActive) }
        binding.activeCard.setOnClickListener { selectActivityLevel(ActivityLevel.Active) }
        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
        binding.nextButton.setOnClickListener {
            selectedActivityLevel?.let { level ->
                userViewModel.updateOnboardingData { it.copy(activityLevel = level.value) }
            }
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    private fun clearClickListeners() {
        binding.sedentaryCard.setOnClickListener(null)
        binding.lightlyActiveCard.setOnClickListener(null)
        binding.activeCard.setOnClickListener(null)
        binding.backButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
    }

    private fun selectActivityLevel(level: ActivityLevel) {
        selectedActivityLevel = level

        val context = requireContext()
        val primaryDarkColor = ContextCompat.getColor(context, R.color.primary_dark)
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(context, R.color.dark_gray)

        val transition = TransitionSet().apply {
            addTransition(Fade())
            addTransition(ChangeBounds())
            interpolator = AccelerateInterpolator()
            duration = 200
        }

        TransitionManager.beginDelayedTransition(binding.radioGroupContainer, transition)

        val allItems = listOf(
            Triple(binding.sedentaryCard, binding.radioSedentary, binding.sedentaryDescription),
            Triple(binding.lightlyActiveCard, binding.radioLightlyActive, binding.lightlyActiveDescription),
            Triple(binding.activeCard, binding.radioActive, binding.activeDescription)
        )

        allItems.forEach { (card, radio, desc) ->
            val isSelected = when(level) {
                ActivityLevel.Sedentary -> card == binding.sedentaryCard
                ActivityLevel.LightlyActive -> card == binding.lightlyActiveCard
                ActivityLevel.Active -> card == binding.activeCard
            }

            card.setCardBackgroundColor(if (isSelected) primaryDarkColor else whiteColor)
            radio.setTextColor(if (isSelected) whiteColor else darkGrayColor)
            radio.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isSelected) selectedTextSize else defaultTextSize)
            desc.visibility = if (isSelected) View.VISIBLE else View.GONE
        }

        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
    }

    private fun disableNextButton() {
        binding.nextButton.isEnabled = false
        binding.nextButton.alpha = 0.5f
    }
}

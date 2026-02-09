package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentSixthScreenBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint

private sealed class UserGoal(val value: String) {
    object LoseWeight : UserGoal("Lose Weight")
    object BuildMuscle : UserGoal("Build Muscle")
    object KeepFit : UserGoal("Keep Fit")
}

@AndroidEntryPoint
class SixthScreen : BaseBindingFragment<FragmentSixthScreenBinding>(FragmentSixthScreenBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private var selectedGoal: UserGoal? = null
    private var initialValueRestored = false

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
                val previousGoal = when (user?.goal) {
                    UserGoal.LoseWeight.value -> UserGoal.LoseWeight
                    UserGoal.BuildMuscle.value -> UserGoal.BuildMuscle
                    UserGoal.KeepFit.value -> UserGoal.KeepFit
                    else -> null
                }

                if (previousGoal != null) {
                    handleCardSelection(previousGoal)
                } else {
                    disableNextButton()
                }
                initialValueRestored = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.loseWeightCard.setOnClickListener { handleCardSelection(UserGoal.LoseWeight) }
        binding.buildMuscleCard.setOnClickListener { handleCardSelection(UserGoal.BuildMuscle) }
        binding.keepFitCard.setOnClickListener { handleCardSelection(UserGoal.KeepFit) }

        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }

        binding.nextButton.setOnClickListener {
            saveDataAndFinish()
        }
    }

    private fun clearClickListeners() {
        with(binding) {
            loseWeightCard.setOnClickListener(null)
            buildMuscleCard.setOnClickListener(null)
            keepFitCard.setOnClickListener(null)
            backButton.setOnClickListener(null)
            nextButton.setOnClickListener(null)
        }
    }

    private fun handleCardSelection(goal: UserGoal) {
        selectedGoal = goal

        val context = requireContext()
        val primaryDarkColor = ContextCompat.getColor(context, R.color.primary_dark)
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(context, R.color.dark_gray)

        val uiMap = mapOf(
            UserGoal.LoseWeight to (binding.loseWeightCard to binding.radioLoseWeight),
            UserGoal.BuildMuscle to (binding.buildMuscleCard to binding.radioBuildMuscle),
            UserGoal.KeepFit to (binding.keepFitCard to binding.radioKeepFit)
        )

        uiMap.forEach { (type, views) ->
            val isSelected = type == goal
            val (card, radio) = views
            card.setCardBackgroundColor(if (isSelected) primaryDarkColor else whiteColor)
            radio.setTextColor(if (isSelected) whiteColor else darkGrayColor)
            radio.isChecked = isSelected
        }

        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
    }

    private fun saveDataAndFinish() {
        selectedGoal?.let { goal ->
            userViewModel.updateOnboardingData { it.copy(goal = goal.value) }
        }
        parentFragmentManager.setFragmentResult("navigationRequestNext", Bundle())
    }

    private fun disableNextButton() {
        binding.nextButton.isEnabled = false
        binding.nextButton.alpha = 0.5f
    }
}

package com.example.nutriority.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import com.example.nutriority.BaseFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentSixthScreenBinding
import com.google.android.material.card.MaterialCardView
import com.example.nutriority.utils.applySystemBarsInsets

private sealed class UserGoal(val value: String) {
    object LoseWeight : UserGoal("Lose Weight")
    object BuildMuscle : UserGoal("Build Muscle")
    object KeepFit : UserGoal("Keep Fit")
}

class SixthScreen : BaseFragment() {

    private var _binding: FragmentSixthScreenBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private var selectedGoal: UserGoal? = null
    private var initialValueRestored = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSixthScreenBinding.inflate(inflater, container, false)
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

    // --- THE FIX: PART 3 ---
    // A new function to nullify all listeners, preventing ghost clicks and memory leaks.
    private fun clearClickListeners() {
        binding.loseWeightCard.setOnClickListener(null)
        binding.buildMuscleCard.setOnClickListener(null)
        binding.keepFitCard.setOnClickListener(null)
        binding.backButton.setOnClickListener(null)
        binding.skipButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
    }

    private fun handleCardSelection(goal: UserGoal) {
        selectedGoal = goal

        val lightGreenColor = ContextCompat.getColor(requireContext(), R.color.green)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)

        val uiMap = mapOf(
            UserGoal.LoseWeight to (binding.loseWeightCard to binding.radioLoseWeight),
            UserGoal.BuildMuscle to (binding.buildMuscleCard to binding.radioBuildMuscle),
            UserGoal.KeepFit to (binding.keepFitCard to binding.radioKeepFit)
        )

        uiMap.values.forEach { (card, radioButton) ->
            card.setCardBackgroundColor(whiteColor)
            radioButton.setTextColor(darkGrayColor)
            radioButton.isChecked = false
        }

        uiMap[goal]?.let { (card, radioButton) ->
            card.setCardBackgroundColor(lightGreenColor)
            radioButton.setTextColor(whiteColor)
            radioButton.isChecked = true
        }

        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
    }

    private fun saveDataAndFinish() {
        selectedGoal?.let { goal ->
            userViewModel.updateOnboardingData { currentUserState ->
                currentUserState.copy(goal = goal.value)
            }
        }
        userViewModel.saveOnboardingData()
        parentFragmentManager.setFragmentResult("navigationRequestNext", Bundle())
    }

    private fun disableNextButton() {
        binding.nextButton.isEnabled = false
        binding.nextButton.alpha = 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

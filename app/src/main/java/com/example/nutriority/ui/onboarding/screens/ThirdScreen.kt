package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentThirdScreenBinding
import dagger.hilt.android.AndroidEntryPoint

private sealed class ActivityLevel(val value: String) {
    object Sedentary : ActivityLevel("Sedentary")
    object LightlyActive : ActivityLevel("Lightly Active")
    object Active : ActivityLevel("Active")
}

@AndroidEntryPoint
class ThirdScreen : Fragment() {

    private var _binding: FragmentThirdScreenBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private var selectedActivityLevel: ActivityLevel? = null
    private var initialValueRestored = false

    private val defaultTextSize = 18f
    private val selectedTextSize = 22f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThirdScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAndSetInitialState()
        // Listeners are no longer set up here
    }

    // --- THE FIX: PART 1 ---
    // Listeners are set up when the fragment is fully visible and interactive.
    override fun onResume() {
        super.onResume()
        setupClickListeners()
    }

    // --- THE FIX: PART 2 ---
    // Listeners are detached when the fragment is paused, preventing ghost clicks.
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
        binding.sedentaryCard.setOnClickListener {
            selectActivityLevel(ActivityLevel.Sedentary)
        }
        binding.lightlyActiveCard.setOnClickListener {
            selectActivityLevel(ActivityLevel.LightlyActive)
        }
        binding.activeCard.setOnClickListener {
            selectActivityLevel(ActivityLevel.Active)
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }

        binding.nextButton.setOnClickListener {
            selectedActivityLevel?.let { level ->
                userViewModel.updateOnboardingData { currentUserState ->
                    currentUserState.copy(activityLevel = level.value)
                }
            }
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    // --- THE FIX: PART 3 ---
    // A new function to nullify all click listeners.
    private fun clearClickListeners() {
        binding.sedentaryCard.setOnClickListener(null)
        binding.lightlyActiveCard.setOnClickListener(null)
        binding.activeCard.setOnClickListener(null)
        binding.backButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
    }

    private fun selectActivityLevel(level: ActivityLevel) {
        selectedActivityLevel = level

        val primaryDarkColor = ContextCompat.getColor(requireContext(), R.color.primary_dark)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)

        val easeInTransition = TransitionSet().apply {
            addTransition(Fade())
            addTransition(ChangeBounds())
            interpolator = AccelerateInterpolator()
            duration = 200
        }

        TransitionManager.beginDelayedTransition(binding.radioGroupContainer, easeInTransition)

        val allCards = listOf(
            Triple(binding.sedentaryCard, binding.radioSedentary, binding.sedentaryDescription),
            Triple(binding.lightlyActiveCard, binding.radioLightlyActive, binding.lightlyActiveDescription),
            Triple(binding.activeCard, binding.radioActive, binding.activeDescription)
        )
        allCards.forEach { (card, radioButton, description) ->
            card.setCardBackgroundColor(whiteColor)
            radioButton.setTextColor(darkGrayColor)
            radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize)
            description.visibility = View.GONE
        }

        val (clickedCard, clickedRadioButton, clickedDescription) = when (level) {
            ActivityLevel.Sedentary -> Triple(binding.sedentaryCard, binding.radioSedentary, binding.sedentaryDescription)
            ActivityLevel.LightlyActive -> Triple(binding.lightlyActiveCard, binding.radioLightlyActive, binding.lightlyActiveDescription)
            ActivityLevel.Active -> Triple(binding.activeCard, binding.radioActive, binding.activeDescription)
        }

        clickedCard.setCardBackgroundColor(primaryDarkColor)
        clickedRadioButton.setTextColor(whiteColor)
        clickedRadioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, selectedTextSize)
        clickedDescription.visibility = View.VISIBLE

        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
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

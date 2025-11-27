package com.example.nutriority.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentWorkoutPreferenceBinding

private sealed class WorkoutPreferenceType(val value: String) {
    object Home : WorkoutPreferenceType("Home")
    object Gym : WorkoutPreferenceType("Gym")
}

class WorkoutPreference : Fragment() {

    private var _binding: FragmentWorkoutPreferenceBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private var selectedPreference: WorkoutPreferenceType? = null
    private var initialValueRestored = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutPreferenceBinding.inflate(inflater, container, false)
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
                val previousPreference = when (user?.workoutPreference) {
                    WorkoutPreferenceType.Home.value -> WorkoutPreferenceType.Home
                    WorkoutPreferenceType.Gym.value -> WorkoutPreferenceType.Gym
                    else -> null
                }

                if (previousPreference != null) {
                    handleCardSelection(previousPreference)
                } else {
                    disableNextButton()
                }
                initialValueRestored = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.homeWorkoutCard.setOnClickListener {
            handleCardSelection(WorkoutPreferenceType.Home)
        }

        binding.gymWorkoutCard.setOnClickListener {
            handleCardSelection(WorkoutPreferenceType.Gym)
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }

        binding.nextButton.setOnClickListener {
            selectedPreference?.let { preference ->
                userViewModel.updateOnboardingData { currentUserState ->
                    currentUserState.copy(workoutPreference = preference.value)
                }
            }
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    // --- THE FIX: PART 3 ---
    // A new function to nullify all listeners, preventing ghost clicks and memory leaks.
    private fun clearClickListeners() {
        binding.homeWorkoutCard.setOnClickListener(null)
        binding.gymWorkoutCard.setOnClickListener(null)
        binding.backButton.setOnClickListener(null)
        binding.skipButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
    }

    private fun handleCardSelection(preference: WorkoutPreferenceType) {
        selectedPreference = preference

        val lightGreenColor = ContextCompat.getColor(requireContext(), R.color.green)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)

        val isHomeSelected = preference is WorkoutPreferenceType.Home

        binding.homeWorkoutCard.setCardBackgroundColor(if (isHomeSelected) lightGreenColor else whiteColor)
        binding.gymWorkoutCard.setCardBackgroundColor(if (isHomeSelected) whiteColor else lightGreenColor)

        binding.radioHomeWorkout.setTextColor(if (isHomeSelected) whiteColor else darkGrayColor)
        binding.radioGymWorkout.setTextColor(if (isHomeSelected) darkGrayColor else whiteColor)

        binding.radioHomeWorkout.isChecked = isHomeSelected
        binding.radioGymWorkout.isChecked = !isHomeSelected

        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
    }

    private fun disableNextButton() {
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)

        binding.homeWorkoutCard.setCardBackgroundColor(whiteColor)
        binding.gymWorkoutCard.setCardBackgroundColor(whiteColor)

        binding.radioHomeWorkout.setTextColor(darkGrayColor)
        binding.radioGymWorkout.setTextColor(darkGrayColor)

        binding.nextButton.isEnabled = false
        binding.nextButton.alpha = 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

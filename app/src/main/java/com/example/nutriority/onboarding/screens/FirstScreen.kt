package com.example.nutriority.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentFirstScreenBinding

// Using a sealed class for type-safe state management is excellent practice.
private sealed class Gender(val value: String) {
    object Male : Gender("Male")
    object Female : Gender("Female")
}

class FirstScreen : Fragment() {

    private var _binding: FragmentFirstScreenBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    // This local state tracks the user's selection within this screen.
    private var selectedGender: Gender? = null
    // This flag prevents the observer from overriding the user's current choice.
    private var initialValueRestored = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set up the observer here. It's safe because it's tied to the viewLifecycleOwner.
        observeAndSetInitialState()
    }

    // --- THE FIX: PART 1 ---
    // setupClickListeners() is now called when the fragment becomes fully visible.
    override fun onResume() {
        super.onResume()
        setupClickListeners()
    }

    // --- THE FIX: PART 2 ---
    // Listeners are cleared when the fragment is no longer the primary visible one.
    // This prevents clicks from being registered on a fragment that is animating away.
    override fun onPause() {
        super.onPause()
        clearClickListeners()
    }

    /**
     * Observes the user data to restore the UI state when the screen is first loaded
     * or revisited, without causing null pointer exceptions or overriding new user input.
     */
    private fun observeAndSetInitialState() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            // The `user` can be null initially. We must handle that.
            // Also, we only restore the state ONCE.
            if (user == null && !initialValueRestored) {
                // If the user object is null (e.g., after clearing data),
                // ensure the 'Next' button is disabled.
                disableNextButton()
            } else if (user != null && !initialValueRestored) {
                // If the user object exists, restore the previous selection.
                when (user.gender) {
                    Gender.Male.value -> selectGender(Gender.Male)
                    Gender.Female.value -> selectGender(Gender.Female)
                    else -> disableNextButton() // No previous gender was saved.
                }
            }
            // Once we have processed the initial state (null or not),
            // we mark it as restored to prevent this logic from running again.
            initialValueRestored = true
        }
    }

    private fun setupClickListeners() {
        binding.maleGroup.setOnClickListener {
            selectGender(Gender.Male)
        }

        binding.femaleGroup.setOnClickListener {
            selectGender(Gender.Female)
        }

        binding.nextButton.setOnClickListener {
            // Only update the ViewModel if a gender has been selected.
            selectedGender?.let { gender ->
                userViewModel.updateOnboardingData { currentUserState ->
                    currentUserState.copy(gender = gender.value)
                }
            }
            // Request navigation to the next screen.
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    // --- THE FIX: PART 3 ---
    // This new function detaches the listeners to prevent memory leaks and ghost clicks.
    private fun clearClickListeners() {
        binding.maleGroup.setOnClickListener(null)
        binding.femaleGroup.setOnClickListener(null)
        binding.skipButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
    }


    /**
     * Main UI logic function. Updates local state and refreshes the UI.
     */
    private fun selectGender(gender: Gender) {
        selectedGender = gender

        // Update UI based on the new state
        val isMale = gender is Gender.Male
        binding.maleGroup.alpha = if (isMale) 1.0f else 0.5f
        binding.femaleGroup.alpha = if (isMale) 0.5f else 1.0f

        // Enable the "Next" button as a selection has been made.
        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
    }

    /**
     * Resets the next button to its initial disabled state.
     */
    private fun disableNextButton() {
        binding.nextButton.isEnabled = false
        binding.nextButton.alpha = 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentFirstScreenBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint

private sealed class Gender(val value: String) {
    object Male : Gender("Male")
    object Female : Gender("Female")
}

@AndroidEntryPoint
class FirstScreen : BaseBindingFragment<FragmentFirstScreenBinding>(FragmentFirstScreenBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private var selectedGender: Gender? = null
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
                if (user == null) {
                    disableNextButton()
                } else {
                    when (user.gender) {
                        Gender.Male.value -> selectGender(Gender.Male)
                        Gender.Female.value -> selectGender(Gender.Female)
                        else -> disableNextButton()
                    }
                }
                initialValueRestored = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.maleGroup.setOnClickListener { selectGender(Gender.Male) }
        binding.femaleGroup.setOnClickListener { selectGender(Gender.Female) }
        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
        binding.nextButton.setOnClickListener {
            selectedGender?.let { gender ->
                userViewModel.updateOnboardingData { it.copy(gender = gender.value) }
            }
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    private fun clearClickListeners() {
        binding.maleGroup.setOnClickListener(null)
        binding.femaleGroup.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
        binding.backButton.setOnClickListener(null)
    }

    private fun selectGender(gender: Gender) {
        selectedGender = gender
        val isMale = gender is Gender.Male
        binding.maleGroup.alpha = if (isMale) 1.0f else 0.5f
        binding.femaleGroup.alpha = if (isMale) 0.5f else 1.0f
        binding.nextButton.isEnabled = true
        binding.nextButton.alpha = 1.0f
    }

    private fun disableNextButton() {
        binding.nextButton.isEnabled = false
        binding.nextButton.alpha = 0.5f
    }
}

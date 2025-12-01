package com.example.nutriority.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nutriority.BaseFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentAgeBinding

class AgeFragment : BaseFragment() {
    private var _binding: FragmentAgeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()
    private var initialValueRestored = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAndSetInitialState()
        setupClickListeners()
    }

    private fun observeAndSetInitialState() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (!initialValueRestored) {
                user?.age?.let { age ->
                    if (age > 0) binding.ageInput.setText(age.toString())
                }
                initialValueRestored = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }

        binding.nextButton.setOnClickListener {
            val ageText = binding.ageInput.text.toString()
            val age = ageText.toIntOrNull()
            if (age != null && age > 0) {
                userViewModel.updateOnboardingData { currentUserState ->
                    currentUserState.copy(age = age)
                }
                setFragmentResult("navigationRequestNext", Bundle())
            } else {
                binding.ageInput.error = "Please enter a valid age"
            }
        }

        binding.skipButton.setOnClickListener {
            // allow skipping age; just continue
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

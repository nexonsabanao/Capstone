package com.example.nutriority.ui.onboarding.screens

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentAgeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AgeFragment : Fragment() {
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
            hideKeyboard()
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }

        binding.nextButton.setOnClickListener {
            validateAndProceed()
        }

        binding.ageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                validateAndProceed()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun validateAndProceed() {
        val ageText = binding.ageInput.text.toString()
        val age = ageText.toIntOrNull()
        when {
            age == null -> {
                binding.ageInputLayout.error = getString(R.string.age_error_invalid)
            }
            age < 15 -> {
                binding.ageInputLayout.error = getString(R.string.age_error_underage)
            }
            age > 80 -> {
                binding.ageInputLayout.error = getString(R.string.age_error_over_limit)
            }
            else -> {
                hideKeyboard()
                binding.ageInputLayout.error = null
                userViewModel.updateOnboardingData { currentUserState ->
                    currentUserState.copy(age = age)
                }
                setFragmentResult("navigationRequestNext", Bundle())
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

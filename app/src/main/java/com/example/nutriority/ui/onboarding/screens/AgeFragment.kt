package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentAgeBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.DatePickerUtil
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AgeFragment : BaseBindingFragment<FragmentAgeBinding>(FragmentAgeBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private var selectedDateInMillis: Long? = null
    private var calculatedAge: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }

        binding.datePickerButton.setOnClickListener {
            DatePickerUtil.showDatePicker(requireContext(), selectedDateInMillis) { selection ->
                selectedDateInMillis = selection
                updateDateUI(selection)
            }
        }

        binding.nextButton.setOnClickListener {
            if (validateAge()) {
                userViewModel.updateOnboardingData { currentUserState ->
                    currentUserState.copy(birthDate = selectedDateInMillis)
                }
                setFragmentResult("navigationRequestNext", Bundle())
            }
        }
    }

    private fun updateDateUI(selection: Long) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = sdf.format(Date(selection))

        calculatedAge = calculateAgeFromMillis(selection)
        
        if (calculatedAge >= 0) {
            binding.tvCalculatedAge.visibility = View.VISIBLE
            binding.tvCalculatedAge.text = "Age: $calculatedAge years old"
            validateAge()
        }
    }

    private fun calculateAgeFromMillis(millis: Long): Int {
        val dob = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    private fun validateAge(): Boolean {
        if (selectedDateInMillis == null) return false

        return when {
            calculatedAge < 15 -> {
                binding.tvDateError.text = "You must be at least 15 years old"
                binding.tvDateError.visibility = View.VISIBLE
                binding.nextButton.isEnabled = false
                false
            }
            calculatedAge > 100 -> {
                binding.tvDateError.text = "Please enter a valid birthdate"
                binding.tvDateError.visibility = View.VISIBLE
                binding.nextButton.isEnabled = false
                false
            }
            else -> {
                binding.tvDateError.visibility = View.GONE
                binding.nextButton.isEnabled = true
                true
            }
        }
    }
}

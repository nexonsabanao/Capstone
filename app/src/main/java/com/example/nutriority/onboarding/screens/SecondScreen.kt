package com.example.nutriority.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentSecondScreenBinding
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlin.math.roundToInt

class SecondScreen : Fragment() {

    private var _binding: FragmentSecondScreenBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private val heightCmRange = (100f..250f)
    private val weightKgRange = (30f..150f)
    private val heightInchesRange = (40f..98f)
    private val weightLbsRange = (66f..330f)

    private var isHeightImperial = false
    private var isWeightImperial = false
    private var currentHeightCm: Double = 170.0
    private var currentWeightKg: Double = 70.0
    private var initialValuesRestored = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRulerViews()
        observeAndSetInitialValues()
    }

    override fun onResume() {
        super.onResume()
        setupClickListeners()
    }

    override fun onPause() {
        super.onPause()
        clearClickListeners()
    }

    private fun observeAndSetInitialValues() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (!initialValuesRestored) {
                user?.let {
                    currentHeightCm = it.heightCm.takeIf { cm -> cm > 0 } ?: currentHeightCm
                    currentWeightKg = it.weightKg.takeIf { kg -> kg > 0 } ?: currentWeightKg
                }
                setupInitialUnitState()
                validateInputs()
                initialValuesRestored = true
            }
        }
    }

    private fun setupRulerViews() {
        binding.heightRulerView.onValueChangedListener = { value ->
            updateHeight(value)
            validateInputs()
        }

        binding.weightRulerView.onValueChangedListener = { value ->
            updateWeight(value)
            validateInputs()
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
        binding.nextButton.setOnClickListener {
            val selectedUnitSystem = if (isHeightImperial || isWeightImperial) "IMPERIAL" else "METRIC"
            userViewModel.updateOnboardingData { user ->
                user.copy(
                    heightCm = currentHeightCm,
                    weightKg = currentWeightKg,
                    unitSystem = selectedUnitSystem
                )
            }
            setFragmentResult("navigationRequestNext", Bundle())
        }

        binding.heightUnitToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                changeHeightUnit(checkedId == R.id.btnFt)
            }
        }

        binding.weightUnitToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                changeWeightUnit(checkedId == R.id.btnLbs)
            }
        }
    }

    private fun clearClickListeners() {
        binding.backButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
        binding.heightUnitToggleGroup.clearOnButtonCheckedListeners()
        binding.weightUnitToggleGroup.clearOnButtonCheckedListeners()
    }

    private fun setupInitialUnitState() {
        binding.heightUnitToggleGroup.check(R.id.btnCm)
        changeHeightUnit(false)

        binding.weightUnitToggleGroup.check(R.id.btnKg)
        changeWeightUnit(false)
    }

    private fun changeHeightUnit(isImperial: Boolean) {
        isHeightImperial = isImperial
        val ruler = binding.heightRulerView
        if (isImperial) {
            ruler.setMajorTickFactor(12)
            ruler.labelFormatter = { value -> "${(value / 12).toInt()}'" }
            ruler.setMinValue(heightInchesRange.start)
            ruler.setMaxValue(heightInchesRange.endInclusive)
            val inches = (currentHeightCm / 2.54).toFloat()
            val coercedInches = inches.coerceIn(heightInchesRange.start, heightInchesRange.endInclusive)
            ruler.setCurrentValue(coercedInches)
            updateHeight(coercedInches)
        } else {
            ruler.setMajorTickFactor(10)
            ruler.labelFormatter = { value -> "${value.toInt()}" }
            ruler.setMinValue(heightCmRange.start)
            ruler.setMaxValue(heightCmRange.endInclusive)
            val cm = currentHeightCm.toFloat()
            val coercedCm = cm.coerceIn(heightCmRange.start, heightCmRange.endInclusive)
            ruler.setCurrentValue(coercedCm)
            updateHeight(coercedCm)
        }
        updateButtonTextColors(binding.heightUnitToggleGroup)
    }

    private fun changeWeightUnit(isImperial: Boolean) {
        isWeightImperial = isImperial
        val ruler = binding.weightRulerView
        ruler.setMajorTickFactor(10)
        ruler.labelFormatter = { value -> "${value.toInt()}" }
        if (isImperial) {
            ruler.setMinValue(weightLbsRange.start)
            ruler.setMaxValue(weightLbsRange.endInclusive)
            val lbs = (currentWeightKg * 2.20462).toFloat()
            val coercedLbs = lbs.coerceIn(weightLbsRange.start, weightLbsRange.endInclusive)
            ruler.setCurrentValue(coercedLbs)
            updateWeight(coercedLbs)
        } else {
            ruler.setMinValue(weightKgRange.start)
            ruler.setMaxValue(weightKgRange.endInclusive)
            val kg = currentWeightKg.toFloat()
            val coercedKg = kg.coerceIn(weightKgRange.start, weightKgRange.endInclusive)
            ruler.setCurrentValue(coercedKg)
            updateWeight(coercedKg)
        }
        updateButtonTextColors(binding.weightUnitToggleGroup)
    }

    private fun updateHeight(value: Float) {
        currentHeightCm = if (isHeightImperial) value * 2.54 else value.toDouble()
        binding.heightValue.text = if (isHeightImperial) formatInchesToFeetAndInches(value) else value.roundToInt().toString()
        binding.heightUnit.text = if (isHeightImperial) "ft" else "cm"
    }

    private fun updateWeight(value: Float) {
        currentWeightKg = if (isWeightImperial) value / 2.20462 else value.toDouble()
        binding.weightValue.text = String.format("%.1f", value)
        binding.weightUnit.text = if (isWeightImperial) "lbs" else "kg"
    }

    private fun formatInchesToFeetAndInches(totalInches: Float): String {
        val feet = (totalInches / 12).toInt()
        val inches = (totalInches % 12).roundToInt()
        return "$feet'$inches'"
    }

    private fun validateInputs() {
        val isValid = currentHeightCm > 0.0 && currentWeightKg > 0.0
        binding.nextButton.isEnabled = isValid
        binding.nextButton.alpha = if (isValid) 1.0f else 0.5f
    }

    private fun updateButtonTextColors(group: MaterialButtonToggleGroup) {
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val darkGrayColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
        for (i in 0 until group.childCount) {
            val button = group.getChildAt(i) as? Button
            button?.setTextColor(if (button.id == group.checkedButtonId) whiteColor else darkGrayColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

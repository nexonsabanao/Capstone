package com.example.nutriority.ui.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.nutriority.R
import com.example.nutriority.databinding.LayoutLogWeightBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.*

class WeightLogBottomSheetFragment(
    private val initialWeightKg: Double,
    private val showDatePicker: Boolean = true,
    private val onWeightLogged: (Double, Long) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutLogWeightBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private val selectedCalendar = Calendar.getInstance()
    private var currentWeightKg: Double = initialWeightKg
    private var isImperial = false

    private val weightKgRange = (30f..150f)
    private val weightLbsRange = (66f..330f)

    companion object {
        private const val LBS_PER_KG = 2.20462
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutLogWeightBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInitialState()
        setupDatePicker()
        setupRuler()
        setupToggle()
        
        binding.btnSave.setOnClickListener {
            onWeightLogged(currentWeightKg, selectedCalendar.timeInMillis)
            dismiss()
        }
    }

    private fun setupInitialState() {
        updateDateDisplay()
        binding.btnDatePicker.isVisible = showDatePicker
        binding.weightUnitToggle.check(R.id.btn_kg)
        changeWeightUnit(false)
    }

    private fun setupDatePicker() {
        binding.btnDatePicker.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedCalendar.set(year, month, dayOfMonth)
                    updateDateDisplay()
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = sdf.format(selectedCalendar.time)
    }

    private fun setupRuler() {
        binding.weightRuler.onValueChangedListener = { value ->
            updateWeight(value)
        }
    }

    private fun setupToggle() {
        binding.weightUnitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                changeWeightUnit(checkedId == R.id.btn_lbs)
            }
        }
    }

    private fun changeWeightUnit(isImperial: Boolean) {
        this.isImperial = isImperial
        val ruler = binding.weightRuler
        val listener = ruler.onValueChangedListener
        ruler.onValueChangedListener = null

        ruler.setMajorTickFactor(10)
        ruler.labelFormatter = { value -> "${value.toInt()}" }

        if (isImperial) {
            ruler.setMinValue(weightLbsRange.start)
            ruler.setMaxValue(weightLbsRange.endInclusive)
            val lbs = (currentWeightKg * LBS_PER_KG).toFloat()
            // If weight is 0 or less, we want RulerView to handle midpoint
            val coercedValue = if (currentWeightKg <= 0) 0f else lbs.coerceIn(weightLbsRange.start, weightLbsRange.endInclusive)
            ruler.setCurrentValue(coercedValue)
            updateWeight(coercedValue)
        } else {
            ruler.setMinValue(weightKgRange.start)
            ruler.setMaxValue(weightKgRange.endInclusive)
            val kg = currentWeightKg.toFloat()
            // If weight is 0 or less, we want RulerView to handle midpoint
            val coercedValue = if (currentWeightKg <= 0) 0f else kg.coerceIn(weightKgRange.start, weightKgRange.endInclusive)
            ruler.setCurrentValue(coercedValue)
            updateWeight(coercedValue)
        }
        updateButtonTextColors(binding.weightUnitToggle)
        ruler.onValueChangedListener = listener
    }

    private fun updateWeight(value: Float) {
        // If value is 0 (handled by RulerView as midpoint), we display that midpoint value correctly
        val finalValue = if (value <= 0f) (binding.weightRuler.getValue()) else value
        currentWeightKg = if (isImperial) finalValue / LBS_PER_KG else finalValue.toDouble()
        binding.tvWeightValue.text = String.format("%.1f", finalValue)
        binding.tvUnitLabel.text = if (isImperial) "lbs" else "kg"
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

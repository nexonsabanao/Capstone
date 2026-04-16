package com.example.nutriority.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import com.example.nutriority.R
import com.example.nutriority.databinding.LayoutLogHeightBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import java.util.Locale
import kotlin.math.roundToInt

class HeightLogBottomSheetFragment(
    private val initialHeightCm: Double,
    private val onHeightLogged: (Double) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutLogHeightBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private var currentHeightCm: Double = initialHeightCm
    private var isImperial = false

    private val heightCmRange = (100f..250f)
    private val heightInchesRange = (40f..98f)

    companion object {
        private const val CM_PER_INCH = 2.54
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutLogHeightBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInitialState()
        setupRuler()
        setupToggle()
        
        binding.btnSave.setOnClickListener {
            onHeightLogged(currentHeightCm)
            dismiss()
        }
    }

    private fun setupInitialState() {
        binding.heightUnitToggle.check(R.id.btn_cm)
        changeHeightUnit(false)
    }

    private fun setupRuler() {
        binding.heightRuler.onValueChangedListener = { value ->
            updateHeight(value)
        }
    }

    private fun setupToggle() {
        binding.heightUnitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                changeHeightUnit(checkedId == R.id.btn_ft)
            }
        }
    }

    private fun changeHeightUnit(isImperial: Boolean) {
        this.isImperial = isImperial
        val ruler = binding.heightRuler
        val listener = ruler.onValueChangedListener
        ruler.onValueChangedListener = null

        if (isImperial) {
            ruler.setMajorTickFactor(12)
            ruler.labelFormatter = { value -> "${(value / 12).toInt()}'" }
            ruler.setMinValue(heightInchesRange.start)
            ruler.setMaxValue(heightInchesRange.endInclusive)
            ruler.setDecimalPlaces(0)
            val inches = (currentHeightCm / CM_PER_INCH).toFloat()
            val coercedValue = inches.coerceIn(heightInchesRange.start, heightInchesRange.endInclusive)
            ruler.setCurrentValue(coercedValue)
            updateHeight(coercedValue)
        } else {
            ruler.setMajorTickFactor(10)
            ruler.labelFormatter = { value -> "${value.toInt()}" }
            ruler.setMinValue(heightCmRange.start)
            ruler.setMaxValue(heightCmRange.endInclusive)
            ruler.setDecimalPlaces(0)
            val cm = currentHeightCm.toFloat()
            val coercedValue = cm.coerceIn(heightCmRange.start, heightCmRange.endInclusive)
            ruler.setCurrentValue(coercedValue)
            updateHeight(coercedValue)
        }
        updateButtonTextColors(binding.heightUnitToggle)
        ruler.onValueChangedListener = listener
    }

    private fun updateHeight(value: Float) {
        // Ensure the stored value is rounded to the nearest whole CM/Inch to avoid decimals
        currentHeightCm = if (isImperial) {
            (value * CM_PER_INCH).roundToInt().toDouble()
        } else {
            value.roundToInt().toDouble()
        }

        if (isImperial) {
            binding.tvHeightValue.text = formatInchesToFeetAndInches(value)
            binding.tvUnitLabel.text = "ft"
        } else {
            binding.tvHeightValue.text = value.roundToInt().toString()
            binding.tvUnitLabel.text = "cm"
        }
    }

    private fun formatInchesToFeetAndInches(totalInches: Float): String {
        val total = totalInches.roundToInt()
        val feet = total / 12
        val inches = total % 12
        return "$feet'$inches\""
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

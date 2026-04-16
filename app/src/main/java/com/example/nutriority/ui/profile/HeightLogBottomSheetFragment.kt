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

class HeightLogBottomSheetFragment(
    private val initialHeightCm: Double,
    private val onHeightLogged: (Double) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutLogHeightBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private var currentHeightCm: Double = initialHeightCm
    private var isImperial = false

    private val heightCmRange = (50f..250f)
    private val heightInRange = (20f..100f)

    companion object {
        private const val IN_PER_CM = 0.393701
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
                changeHeightUnit(checkedId == R.id.btn_in)
            }
        }
    }

    private fun changeHeightUnit(isImperial: Boolean) {
        this.isImperial = isImperial
        val ruler = binding.heightRuler
        val listener = ruler.onValueChangedListener
        ruler.onValueChangedListener = null

        ruler.setMajorTickFactor(10)
        ruler.labelFormatter = { value -> "${value.toInt()}" }

        if (isImperial) {
            ruler.setMinValue(heightInRange.start)
            ruler.setMaxValue(heightInRange.endInclusive)
            ruler.setDecimalPlaces(1)
            val inches = (currentHeightCm * IN_PER_CM).toFloat()
            val coercedValue = inches.coerceIn(heightInRange.start, heightInRange.endInclusive)
            ruler.setCurrentValue(coercedValue)
            updateHeight(coercedValue)
        } else {
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
        currentHeightCm = if (isImperial) (value / IN_PER_CM) else value.toDouble()
        if (isImperial) {
            binding.tvHeightValue.text = String.format("%.1f", value)
            binding.tvUnitLabel.text = "in"
        } else {
            binding.tvHeightValue.text = String.format("%.0f", value)
            binding.tvUnitLabel.text = "cm"
        }
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

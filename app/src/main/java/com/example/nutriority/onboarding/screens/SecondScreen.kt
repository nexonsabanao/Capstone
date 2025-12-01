package com.example.nutriority.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import com.example.nutriority.BaseFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentSecondScreenBinding
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlin.math.roundToInt

class SecondScreen : BaseFragment() {

    private var _binding: FragmentSecondScreenBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var heightAdapter: PickerAdapter
    private lateinit var weightAdapter: PickerAdapter

    private val heightSnapHelper = LinearSnapHelper()
    private val weightSnapHelper = LinearSnapHelper()

    private val heightCmRange = (120..220).toList()
    private val weightKgRange = (30..200).toList()
    private val heightInchesRange = (48..86).toList()
    private val weightLbsRange = (66..440).toList()

    private var isProgrammaticScroll = false
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
        setupPickers()
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

    private fun setupPickers() {
        val itemWidth = resources.getDimensionPixelSize(R.dimen.picker_item_width)
        val screenWidth = resources.displayMetrics.widthPixels
        val padding = screenWidth / 2 - itemWidth / 2

        heightAdapter = PickerAdapter(emptyList())
        binding.heightPickerRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = heightAdapter
            setPadding(padding, 0, padding, 0)
            heightSnapHelper.attachToRecyclerView(this)
        }

        binding.heightPickerRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isProgrammaticScroll) {
                    val position = getSnapPosition(recyclerView, heightSnapHelper)
                    if (position != RecyclerView.NO_POSITION) {
                        currentHeightCm = if (isHeightImperial) {
                            heightInchesRange[position] * 2.54
                        } else {
                            heightCmRange[position].toDouble()
                        }
                        validateInputs()
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isProgrammaticScroll = false
                }
            }
        })

        weightAdapter = PickerAdapter(emptyList())
        binding.weightPickerRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = weightAdapter
            setPadding(padding, 0, padding, 0)
            weightSnapHelper.attachToRecyclerView(this)
        }

        binding.weightPickerRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isProgrammaticScroll) {
                    val position = getSnapPosition(recyclerView, weightSnapHelper)
                    if (position != RecyclerView.NO_POSITION) {
                        currentWeightKg = if (isWeightImperial) {
                            weightLbsRange[position] / 2.20462
                        } else {
                            weightKgRange[position].toDouble()
                        }
                        validateInputs()
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isProgrammaticScroll = false
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
        binding.nextButton.setOnClickListener {
            // --- START OF APPLIED LOGIC ---

            // If either height OR weight is set to imperial, we consider the whole system Imperial
            // for the purpose of the final recap screen. This handles all mixed-unit cases gracefully.
            val selectedUnitSystem = if (binding.heightUnitToggleGroup.checkedButtonId == R.id.btnFt || binding.weightUnitToggleGroup.checkedButtonId == R.id.btnLbs) {
                "IMPERIAL"
            } else {
                "METRIC"
            }
            userViewModel.updateOnboardingData { user ->
                user.copy(
                    // Your existing logic correctly keeps currentHeightCm and currentWeightKg updated in metric.
                    // We just need to convert them to Float to match the User data class if it uses Float.
                    heightCm = currentHeightCm,
                    weightKg = currentWeightKg,
                    unitSystem = selectedUnitSystem // Save the determined unit system
                )
            }

            // --- END OF APPLIED LOGIC ---

            // This part is correct and remains the same
            setFragmentResult("navigationRequestNext", Bundle())
        }

        binding.btnCm.setOnClickListener {
            if (isHeightImperial) {
                isHeightImperial = false
                changeHeightUnit(false)
            }
        }
        binding.btnFt.setOnClickListener {
            if (!isHeightImperial) {
                isHeightImperial = true
                changeHeightUnit(true)
            }
        }

        binding.btnKg.setOnClickListener {
            if (isWeightImperial) {
                isWeightImperial = false
                changeWeightUnit(false)
            }
        }
        binding.btnLbs.setOnClickListener {
            if (!isWeightImperial) {
                isWeightImperial = true
                changeWeightUnit(true)
            }
        }
    }

    private fun clearClickListeners() {
        binding.backButton.setOnClickListener(null)
        binding.skipButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
        binding.btnCm.setOnClickListener(null)
        binding.btnFt.setOnClickListener(null)
        binding.btnKg.setOnClickListener(null)
        binding.btnLbs.setOnClickListener(null)
    }

    private fun setupInitialUnitState() {
        binding.heightUnitToggleGroup.check(R.id.btnCm)
        changeHeightUnit(false)

        binding.weightUnitToggleGroup.check(R.id.btnKg)
        changeWeightUnit(false)
    }

    private fun changeHeightUnit(isImperial: Boolean) {
        val newList = if (isImperial) {
            heightInchesRange.map { inch ->
                val feet = inch / 12
                val inches = inch % 12
                String.format("%d'%d\"", feet, inches)
            }
        } else {
            heightCmRange.map { it.toString() }
        }
        heightAdapter.updateData(newList)

        val position = if (isImperial) {
            val inches = (currentHeightCm / 2.54).roundToInt()
            heightInchesRange.indexOf(inches.coerceIn(heightInchesRange.first(), heightInchesRange.last()))
        } else {
            heightCmRange.indexOf(currentHeightCm.roundToInt().coerceIn(heightCmRange.first(), heightCmRange.last()))
        }

        scrollToPosition(binding.heightPickerRecyclerView, position)
        updateButtonTextColors(binding.heightUnitToggleGroup)
    }

    private fun changeWeightUnit(isImperial: Boolean) {
        val newList = if (isImperial) {
            weightLbsRange.map { it.toString() }
        } else {
            weightKgRange.map { it.toString() }
        }
        weightAdapter.updateData(newList)

        val position = if (isImperial) {
            val lbs = (currentWeightKg * 2.20462).roundToInt()
            weightLbsRange.indexOf(lbs.coerceIn(weightLbsRange.first(), weightLbsRange.last()))
        } else {
            weightKgRange.indexOf(currentWeightKg.roundToInt().coerceIn(weightKgRange.first(), weightKgRange.last()))
        }

        scrollToPosition(binding.weightPickerRecyclerView, position)
        updateButtonTextColors(binding.weightUnitToggleGroup)
    }

    private fun scrollToPosition(recyclerView: RecyclerView, position: Int) {
        if (position != -1) {
            isProgrammaticScroll = true
            recyclerView.post {
                recyclerView.smoothScrollToPosition(position)
            }
        }
    }

    private fun getSnapPosition(recyclerView: RecyclerView, snapHelper: LinearSnapHelper): Int {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return RecyclerView.NO_POSITION
        val snapView = snapHelper.findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
        return layoutManager.getPosition(snapView)
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

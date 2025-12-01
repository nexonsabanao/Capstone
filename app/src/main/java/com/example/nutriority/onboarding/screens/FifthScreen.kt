package com.example.nutriority.onboarding.screens

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import com.example.nutriority.BaseFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentFifthScreenBinding
import com.google.android.material.chip.Chip
import com.example.nutriority.utils.applySystemBarsInsets

class FifthScreen : BaseFragment() {

    private var _binding: FragmentFifthScreenBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private var initialValueRestored = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFifthScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChipStyle()
        observeAndSetInitialState()
        // Listeners are no longer set up here.
    }

    // --- THE FIX: PART 1 ---
    // Listeners are now set up only when the fragment is fully visible and interactive.
    override fun onResume() {
        super.onResume()
        setupClickListeners()
    }

    // --- THE FIX: PART 2 ---
    // Listeners are detached when the fragment is paused. This is the key to preventing the bug.
    override fun onPause() {
        super.onPause()
        clearClickListeners()
    }

    private fun observeAndSetInitialState() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (!initialValueRestored) {
                user?.excludedIngredients?.let { savedIngredients ->
                    binding.ingredientsChipGroup.forEach { chipView ->
                        (chipView as? Chip)?.let { chip ->
                            val chipText = chip.text.toString().substringAfter(" ").trim()
                            if (savedIngredients.contains(chipText)) {
                                chip.isChecked = true
                            }
                        }
                    }
                }
                initialValueRestored = true
            }
        }
    }

    private fun setupChipStyle() {
        val greenColor = ContextCompat.getColor(requireContext(), R.color.green)
        val defaultBackgroundColor = ContextCompat.getColor(requireContext(), R.color.light_gray_background)
        val defaultTextColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)

        val backgroundStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(greenColor, defaultBackgroundColor)
        )

        val textStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(whiteColor, defaultTextColor)
        )

        binding.ingredientsChipGroup.forEach { chipView ->
            (chipView as? Chip)?.let {
                it.chipBackgroundColor = backgroundStateList
                it.setTextColor(textStateList)
                it.chipStrokeWidth = 0f
                it.isChipIconVisible = false
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }

        binding.finishButton.setOnClickListener {
            saveDataAndFinish()
        }

        binding.nextButton.setOnClickListener {
            saveDataAndFinish()
        }
    }

    // --- THE FIX: PART 3 ---
    // A new function to nullify all listeners, preventing ghost clicks and memory leaks.
    private fun clearClickListeners() {
        binding.backButton.setOnClickListener(null)
        binding.finishButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
    }

    private fun saveDataAndFinish() {
        val excludedIngredients = getSelectedIngredients()

        userViewModel.updateOnboardingData { currentUserState ->
            currentUserState.copy(excludedIngredients = excludedIngredients)
        }

        userViewModel.saveOnboardingData()
        setFragmentResult("navigationRequestNext", Bundle())
    }

    private fun getSelectedIngredients(): List<String> {
        val selectedIngredients = mutableListOf<String>()
        binding.ingredientsChipGroup.checkedChipIds.forEach { id ->
            val chip: Chip? = binding.ingredientsChipGroup.findViewById(id)
            chip?.let {
                val cleanText = it.text.toString().substringAfter(" ").trim()
                selectedIngredients.add(cleanText)
            }
        }
        return selectedIngredients
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.nutriority.ui.onboarding.screens

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentFifthScreenBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FifthScreen : BaseBindingFragment<FragmentFifthScreenBinding>(FragmentFifthScreenBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private var initialValueRestored = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChipStyle()
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
        val context = requireContext()
        val primaryDarkColor = ContextCompat.getColor(context, R.color.primary_dark)
        val defaultBackgroundColor = ContextCompat.getColor(context, R.color.white)
        val defaultTextColor = ContextCompat.getColor(context, R.color.dark_gray)
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        val backgroundStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(primaryDarkColor, defaultBackgroundColor)
        )

        val textStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
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
        binding.nextButton.setOnClickListener { saveDataAndFinish() }
    }

    private fun clearClickListeners() {
        binding.backButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
    }

    private fun saveDataAndFinish() {
        val excludedIngredients = getSelectedIngredients()
        userViewModel.updateOnboardingData { it.copy(excludedIngredients = excludedIngredients) }
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
}

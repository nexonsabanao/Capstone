package com.example.nutriority.ui.onboarding.screens

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentFifthScreenBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
                    val predefinedIngredients = mutableListOf<String>()
                    binding.ingredientsChipGroup.forEach { chipView ->
                        (chipView as? Chip)?.let { chip ->
                            if (chip.id == R.id.addCustomChip) return@let
                            val chipText = chip.text.toString().substringAfter(" ").trim()
                            predefinedIngredients.add(chipText)
                            if (savedIngredients.contains(chipText)) {
                                chip.isChecked = true
                            }
                        }
                    }

                    // Add chips for custom ingredients that weren't in the predefined list
                    savedIngredients.forEach { ingredient ->
                        if (!predefinedIngredients.contains(ingredient)) {
                            addCustomChipToGroup(ingredient, true)
                        }
                    }
                }
                initialValueRestored = true
            }
        }
    }

    private fun setupChipStyle() {
        binding.ingredientsChipGroup.forEach { chipView ->
            (chipView as? Chip)?.let { applyStyleToChip(it) }
        }
    }

    private fun applyStyleToChip(chip: Chip) {
        if (chip.id == R.id.addCustomChip) return

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

        chip.chipBackgroundColor = backgroundStateList
        chip.setTextColor(textStateList)
        chip.chipStrokeWidth = 0f
        chip.isChipIconVisible = false
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
        binding.nextButton.setOnClickListener { saveDataAndFinish() }
        binding.addCustomChip.setOnClickListener { showAddCustomIngredientDialog() }
    }

    private fun showAddCustomIngredientDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_edit_field_bottom_sheet, null)
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvSheetTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvSheetSubtitle)
        val til = dialogView.findViewById<TextInputLayout>(R.id.textInputLayout)
        val etValue = dialogView.findViewById<TextInputEditText>(R.id.etFieldValue)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)

        tvTitle.text = "Add Exclusion"
        tvSubtitle.text = "Enter an ingredient you want to avoid"
        etValue.hint = "e.g. Cilantro, Peanuts"
        
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val ingredient = etValue.text.toString().trim()
            if (ingredient.isNotEmpty()) {
                addCustomChipToGroup(ingredient, true)
                dialog.dismiss()
            } else {
                til.error = "Please enter an ingredient"
            }
        }
        dialog.show()
    }

    private fun addCustomChipToGroup(ingredient: String, isChecked: Boolean) {
        val context = requireContext()
        val chip = Chip(context)
        chip.text = "🏷️ $ingredient"
        chip.isCheckable = true
        chip.isChecked = isChecked
        
        applyStyleToChip(chip)
        
        // Add before the "Add Custom" button
        val index = binding.ingredientsChipGroup.childCount - 1
        binding.ingredientsChipGroup.addView(chip, index)
    }

    private fun clearClickListeners() {
        binding.backButton.setOnClickListener(null)
        binding.nextButton.setOnClickListener(null)
        binding.addCustomChip.setOnClickListener(null)
    }

    private fun saveDataAndFinish() {
        val excludedIngredients = getSelectedIngredients()
        userViewModel.updateOnboardingData { it.copy(excludedIngredients = excludedIngredients) }
        setFragmentResult("navigationRequestNext", Bundle())
    }

    private fun getSelectedIngredients(): List<String> {
        val selectedIngredients = mutableListOf<String>()
        binding.ingredientsChipGroup.forEach { chipView ->
            (chipView as? Chip)?.let { chip ->
                if (chip.isChecked && chip.id != R.id.addCustomChip) {
                    val cleanText = chip.text.toString().substringAfter(" ").trim()
                    selectedIngredients.add(cleanText)
                }
            }
        }
        return selectedIngredients
    }
}

package com.example.nutriority.ui.onboarding.screens

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.TextInputEditText
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
        val context = requireContext()
        
        // Use a 16dp corner radius to match standard chips
        val cornerRadius = resources.getDimension(R.dimen.chip_corner_radius_default) 
        chip.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCornerSizes(cornerRadius)
            .build()

        if (chip.id == R.id.addCustomChip) return

        // Color Logic updated to use NEW specific exclusion selectors
        chip.chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.exclusion_chip_background_selector)
        chip.setTextColor(ContextCompat.getColorStateList(context, R.color.exclusion_chip_text_selector))
        chip.chipStrokeColor = ContextCompat.getColorStateList(context, R.color.exclusion_chip_stroke_selector)
        
        chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width_default)
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_dialog_add_exclusion, null)
        val etIngredient = dialogView.findViewById<TextInputEditText>(R.id.etIngredientName)
        val btnAdd = dialogView.findViewById<MaterialButton>(R.id.btnAdd)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            hideKeyboard(etIngredient)
            dialog.dismiss()
        }

        btnAdd.setOnClickListener {
            val ingredient = etIngredient.text.toString().trim()
            if (ingredient.isNotEmpty()) {
                hideKeyboard(etIngredient)
                addCustomChipToGroup(ingredient, true)
                dialog.dismiss()
            }
        }
        
        dialog.show()
        etIngredient.requestFocus()
        showKeyboard(etIngredient)
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

    private fun showKeyboard(view: View) {
        view.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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

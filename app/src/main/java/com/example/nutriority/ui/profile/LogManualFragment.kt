package com.example.nutriority.ui.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.R
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.databinding.FragmentLogManualBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class LogManualFragment : BaseBindingFragment<FragmentLogManualBinding>(FragmentLogManualBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()
    @Inject lateinit var mealRepository: MealRepository

    private var selectedImageUri: Uri? = null
    private var webpImagePath: String? = null
    private val ingredientsList = mutableListOf<String>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri = uri
            processAndDisplayImage(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdown()
        setupClickListeners()
        updateIngredientsUi()
    }

    private fun setupDropdown() {
        val items = listOf("Breakfast", "Lunch", "Dinner", "Snack")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.spinnerMealTime.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { navigationViewModel.goBack() }
        binding.btnCamera.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
        binding.btnAddIngredient.setOnClickListener { showAddIngredientDialog() }
        binding.btnLogMeal.setOnClickListener { 
            KeyboardUtil.hideKeyboard(requireActivity())
            saveMeal() 
        }
    }

    private fun showAddIngredientDialog() {
        val input = EditText(requireContext()).apply { hint = "Enter ingredient name" }
        AlertDialog.Builder(requireContext())
            .setTitle("Add Ingredient")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    ingredientsList.add(name)
                    updateIngredientsUi()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateIngredientsUi() {
        binding.ingredientsContainer.removeAllViews()
        binding.tvIngredientEmpty.isVisible = ingredientsList.isEmpty()

        ingredientsList.forEachIndexed { index, ingredient ->
            val textView = TextView(requireContext()).apply {
                text = "• $ingredient"
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
                setPadding(0, 8, 0, 8)
                setOnLongClickListener {
                    ingredientsList.removeAt(index)
                    updateIngredientsUi()
                    true
                }
            }
            binding.ingredientsContainer.addView(textView)
        }
    }

    private fun processAndDisplayImage(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) { null }
            }

            bitmap?.let {
                binding.ivMealPreview.setImageBitmap(it)
                binding.ivMealPreview.visibility = View.VISIBLE
                binding.ivCameraIcon.visibility = View.GONE
                webpImagePath = convertToWebP(it)
            }
        }
    }

    private suspend fun convertToWebP(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "meal_${System.currentTimeMillis()}.webp"
            val file = File(requireContext().cacheDir, fileName)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
            out.flush()
            out.close()
            file.absolutePath
        } catch (e: Exception) { null }
    }

    private fun saveMeal() {
        val title = binding.etTitle.text.toString().trim()
        val protein = binding.etProtein.text.toString().toIntOrNull() ?: 0
        val carbs = binding.etCarb.text.toString().toIntOrNull() ?: 0
        val fats = binding.etFat.text.toString().toIntOrNull() ?: 0
        val mealTime = binding.spinnerMealTime.text.toString()

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Please add a title", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                mealRepository.logManualMeal(title, protein, carbs, fats, mealTime, ingredientsList)
                Toast.makeText(requireContext(), "Meal logged successfully!", Toast.LENGTH_SHORT).show()
                navigationViewModel.goBack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to log meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

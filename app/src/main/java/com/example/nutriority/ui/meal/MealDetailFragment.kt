package com.example.nutriority.ui.meal

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.R
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.FragmentMealDetailBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.profile.ProfileViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class MealDetailFragment : BaseBindingFragment<FragmentMealDetailBinding>(FragmentMealDetailBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private var currentMeal: Meal? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { navigationViewModel.goBack() }

        binding.tvToolbarTitle.alpha = 0f
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener

            val percentage = abs(verticalOffset).toFloat() / totalScrollRange
            val startFadeAt = 0.8f
            if (percentage > startFadeAt) {
                val alphaProgress = (percentage - startFadeAt) / (1f - startFadeAt)
                val alphaInt = (alphaProgress * 255).toInt().coerceIn(0, 255)
                binding.toolbar.setBackgroundColor(Color.argb(alphaInt, 255, 255, 255))
                binding.tvToolbarTitle.alpha = alphaProgress
            } else {
                binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
                binding.tvToolbarTitle.alpha = 0f
            }
        })

        setupToggleGroup()
        setupLogButton()
        observeMealData()
    }

    private fun setupLogButton() {
        binding.btnLogMeal.setOnClickListener {
            if (isProfileComplete()) {
                currentMeal?.let { meal ->
                    profileViewModel.logMeal(meal)
                    binding.btnLogMeal.apply {
                        text = "LOGGED"
                        isEnabled = false
                        alpha = 0.7f
                        setIconResource(R.drawable.ic_check_circle)
                    }
                    Toast.makeText(requireContext(), "${meal.name} added to profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isProfileComplete(): Boolean {
        // Use the observed UI state to get current user data
        val user = profileViewModel.uiState.value.user
        if (user == null) {
            Toast.makeText(requireContext(), "Loading user profile...", Toast.LENGTH_SHORT).show()
            return false
        }

        val missingFields = mutableListOf<String>()
        if (user.birthDate == null) missingFields.add("Birth Date")
        if (user.heightCm <= 0) missingFields.add("Height")
        if (user.weightKg <= 0) missingFields.add("Weight")
        if (user.gender.isBlank()) missingFields.add("Gender")
        if (user.activityLevel.isBlank()) missingFields.add("Activity Level")
        if (user.goal.isBlank()) missingFields.add("Fitness Goal")

        return if (missingFields.isNotEmpty()) {
            showProfileIncompleteDialog(missingFields)
            false
        } else {
            true
        }
    }

    private fun showProfileIncompleteDialog(missingFields: List<String>) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_incomplete, null)
        
        val tvMissing = dialogView.findViewById<TextView>(R.id.tvMissingFields)
        val btnGoToProfile = dialogView.findViewById<MaterialButton>(R.id.btnGoToProfile)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        tvMissing.text = "Missing: ${missingFields.joinToString(", ")}"

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnGoToProfile.setOnClickListener {
            dialog.dismiss()
            navigationViewModel.navigateToEditProfile()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeMealData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.selectedMealJson.collect { json ->
                    if (json != null) {
                        currentMeal = Gson().fromJson(json, Meal::class.java)
                        displayMealDetails()
                        binding.btnLogMeal.apply {
                            text = "LOG MEAL"
                            isEnabled = true
                            alpha = 1.0f
                            setIconResource(R.drawable.ic_fire)
                        }
                        binding.nestedScrollView.scrollTo(0, 0)
                        binding.appBarLayout.setExpanded(true)
                    }
                }
            }
        }
    }

    private fun displayMealDetails() {
        currentMeal?.let { meal ->
            binding.tvToolbarTitle.text = meal.name
            binding.mealName.text = meal.name
            binding.mealCalories.text = "${meal.calories} kcal"
            binding.mealTime.text = "${meal.duration} min"
            binding.tvMealInstructions.text = meal.instructions

            val context = requireContext()
            if (meal.imageName.startsWith("http")) {
                com.bumptech.glide.Glide.with(this)
                    .load(meal.imageName)
                    .centerCrop()
                    .placeholder(R.drawable.bg_meal_placeholder)
                    .into(binding.mealImage)
            } else {
                val resId = context.resources.getIdentifier(meal.imageName, "drawable", context.packageName)
                binding.mealImage.setImageResource(if (resId != 0) resId else R.drawable.img_balanced_diet)
            }

            setupNutritionChart(meal)
            
            if (binding.toggleGroup.checkedButtonId == binding.btnIngredients.id) {
                showIngredients()
            } else {
                showInstructions()
            }
        }
    }

    private fun setupNutritionChart(meal: Meal) {
        val entries = ArrayList<PieEntry>()
        val p = meal.macros.protein.toFloat()
        val c = meal.macros.carbs.toFloat()
        val f = meal.macros.fats.toFloat()
        val total = p + c + f

        if (total > 0) {
            entries.add(PieEntry(p, "Protein"))
            entries.add(PieEntry(c, "Carbs"))
            entries.add(PieEntry(f, "Fat"))
        } else {
            entries.add(PieEntry(1f, "No Data"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"), Color.parseColor("#FF5722"))
            setDrawValues(false)
        }

        binding.nutritionChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 70f
            setHoleColor(Color.TRANSPARENT)
            setDrawEntryLabels(false)
            animateY(800)
            invalidate()
        }

        binding.tvProteinPercent.text = "Protein: ${meal.macros.protein}g"
        binding.tvCarbPercent.text = "Carbs: ${meal.macros.carbs}g"
        binding.tvFatPercent.text = "Fat: ${meal.macros.fats}g"
    }

    private fun setupToggleGroup() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnInstructions.id -> showInstructions()
                    binding.btnIngredients.id -> showIngredients()
                }
            }
        }
    }

    private fun showInstructions() {
        binding.sectionTitle.text = "Instructions"
        binding.tvMealInstructions.visibility = View.VISIBLE
        binding.contentRecyclerView.visibility = View.GONE
    }

    private fun showIngredients() {
        binding.sectionTitle.text = "Ingredients"
        binding.tvMealInstructions.visibility = View.GONE
        binding.contentRecyclerView.visibility = View.VISIBLE
        
        currentMeal?.let { meal ->
            binding.contentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.contentRecyclerView.adapter = LoggedIngredientAdapter(meal.ingredients)
        }
    }
}

class LoggedIngredientAdapter(private val ingredients: List<String>) : 
    RecyclerView.Adapter<LoggedIngredientAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = "• ${ingredients[position]}"
        holder.text.textSize = 14f
        holder.text.setTextColor(Color.parseColor("#424242"))
    }

    override fun getItemCount() = ingredients.size
}

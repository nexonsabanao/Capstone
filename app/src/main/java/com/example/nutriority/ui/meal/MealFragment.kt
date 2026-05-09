package com.example.nutriority.ui.meal

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentMealBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@AndroidEntryPoint
class MealFragment : BaseBindingFragment<FragmentMealBinding>(FragmentMealBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val mealViewModel: MealViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val mealAdapter by lazy {
        GeneratedMealPlanAdapter(
            onMealClick = { meal ->
                navigationViewModel.navigateToMealDetail(Gson().toJson(meal))
            },
            onSwapClick = { mealToReplace, dayIndex ->
                mealViewModel.swapMeal(mealToReplace, dayIndex)
            }
        )
    }

    private var hasAutoScrolled = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        
        // Initial state
        updateDateViews(LocalDate.now())
        
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.generatedMealPlanRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mealAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.nextButton.setOnClickListener { validateAndGenerateMealPlan() }
        binding.doneButton.setOnClickListener { 
            mealViewModel.deleteMealPlan()
        }
        binding.btnMenu.setOnClickListener { showPopupMenu(it) }
    }

    private fun validateAndGenerateMealPlan() {
        val user = userViewModel.user.value
        if (user == null) {
            Toast.makeText(requireContext(), "Error: User data not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for mandatory data
        val missingFields = mutableListOf<String>()
        if (user.birthDate == null) missingFields.add("Birth Date")
        if (user.heightCm <= 0) missingFields.add("Height")
        if (user.weightKg <= 0) missingFields.add("Weight")
        if (user.gender.isBlank()) missingFields.add("Gender")
        if (user.activityLevel.isBlank()) missingFields.add("Activity Level")
        if (user.goal.isBlank()) missingFields.add("Fitness Goal")
        if (user.preferredDiet.isBlank()) missingFields.add("Preferred Diet")

        if (missingFields.isNotEmpty()) {
            showProfileIncompleteDialog(missingFields)
        } else {
            // Immediate UI feedback: hide everything and show loader
            binding.initialView.isVisible = false
            binding.generatedMealPlanRecyclerView.isVisible = false
            binding.loadingProgressBar.isVisible = true
            mealViewModel.generateNewMealPlan()
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

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_meal_plan, popup.menu)
        
        // Disable actions while generating
        val isGenerating = mealViewModel.uiState.value.isGenerating
        popup.menu.findItem(R.id.action_regenerate)?.isEnabled = !isGenerating
        popup.menu.findItem(R.id.action_delete_plan)?.isEnabled = !isGenerating

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_plan -> {
                    mealViewModel.deleteMealPlan()
                    Toast.makeText(requireContext(), "Meal plan deleted", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_regenerate -> {
                    validateAndGenerateMealPlan()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    var lastHasPlan: Boolean? = null
                    var wasGenerating = false
                    mealViewModel.uiState.collectLatest { state ->
                        if (state.isInitialLoading) {
                            binding.loadingProgressBar.isVisible = true
                            binding.initialView.isVisible = false
                            binding.generatedMealPlanRecyclerView.isVisible = false
                            binding.btnMenu.isVisible = false
                            return@collectLatest
                        }

                        val isGenerating = state.isGenerating
                        binding.loadingProgressBar.isVisible = isGenerating
                        
                        // Core visibility logic
                        val showPlan = state.hasPlan && !isGenerating
                        val showInitial = !state.hasPlan && !isGenerating
                        
                        binding.initialView.isVisible = showInitial
                        binding.generatedMealPlanRecyclerView.isVisible = showPlan
                        binding.btnMenu.isVisible = showPlan
                        binding.nextButton.isEnabled = !isGenerating
                        
                        // Show "Done" if plan is expired (e.g. 8th day) OR if last day's meals are all logged
                        binding.doneButton.isVisible = showPlan && (state.isPlanExpired || state.isLastDayLogged)
                        
                        // Fix: When the plan is deleted, scroll back to top of the initial view
                        if (lastHasPlan == true && !state.hasPlan) {
                            binding.mealNestedScrollView.scrollTo(0, 0)
                        }
                        lastHasPlan = state.hasPlan
                        
                        state.startDate?.let { updateDateViews(it) }
                        
                        mealAdapter.submitList(state.items) {
                            if (showPlan) {
                                if (wasGenerating) {
                                    binding.mealNestedScrollView.smoothScrollTo(0, 0)
                                    hasAutoScrolled = true
                                } else if (!hasAutoScrolled && state.items.isNotEmpty()) {
                                    scrollToToday(state.items)
                                    hasAutoScrolled = true
                                }
                            } else {
                                hasAutoScrolled = false
                            }
                            wasGenerating = isGenerating
                        }
                    }
                }

                launch {
                    mealViewModel.errorEvents.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }

                launch {
                    mealViewModel.swapState.collectLatest { state ->
                        state?.let {
                            val bottomSheet = MealSwapBottomSheetFragment(
                                mealType = it.mealToReplace.mealTime,
                                options = it.options,
                                onMealSwapped = { newMeal ->
                                    mealViewModel.onSwapMealSelected(it.mealToReplace, newMeal, it.dayIndex)
                                }
                            )
                            bottomSheet.show(parentFragmentManager, "MealSwapBottomSheet")
                            mealViewModel.onSwapCancelled()
                        }
                    }
                }
            }
        }
    }

    private fun scrollToToday(items: List<MealListItem>) {
        val todayIndex = items.indexOfFirst { it is MealListItem.HeaderItem && it.isToday }
        if (todayIndex != -1) {
            binding.generatedMealPlanRecyclerView.post {
                val layoutManager = binding.generatedMealPlanRecyclerView.layoutManager as? LinearLayoutManager
                val view = layoutManager?.findViewByPosition(todayIndex)
                if (view != null) {
                    val scrollY = binding.generatedMealPlanRecyclerView.top + view.top
                    binding.mealNestedScrollView.smoothScrollTo(0, scrollY)
                }
            }
        }
    }

    private fun updateDateViews(startDate: LocalDate) {
        val today = LocalDate.now()
        val endDate = startDate.plusDays(6)
        val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val dayNameFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())

        val startDayLabel = when (ChronoUnit.DAYS.between(today, startDate).toInt()) {
            0 -> "Today"
            -1 -> "Yesterday"
            1 -> "Tomorrow"
            else -> startDate.format(dayNameFormatter)
        }

        binding.startDayName.text = startDayLabel
        binding.startDateText.text = startDate.format(monthDayFormatter)
        binding.endDayName.text = endDate.format(dayNameFormatter)
        binding.endDateText.text = endDate.format(monthDayFormatter)
    }
}

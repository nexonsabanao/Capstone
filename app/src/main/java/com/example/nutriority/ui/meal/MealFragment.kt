package com.example.nutriority.ui.meal

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentMealBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class MealFragment : BaseBindingFragment<FragmentMealBinding>(FragmentMealBinding::inflate) {

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        updateDateViews()
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
        binding.nextButton.setOnClickListener { mealViewModel.generateNewMealPlan() }
        binding.doneButton.setOnClickListener { navigationViewModel.resetToHome() }
        binding.btnMenu.setOnClickListener { showPopupMenu(it) }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_meal_plan, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_plan -> {
                    deleteMealPlan()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun deleteMealPlan() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = mealViewModel.userRepository.getInitialUser()
            if (user != null) {
                mealViewModel.userRepository.insertUser(user.copy(mealPlanJson = null))
                Toast.makeText(requireContext(), "Meal plan deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mealViewModel.isGenerating.observe(viewLifecycleOwner) { isGenerating ->
                        binding.loadingProgressBar.isVisible = isGenerating
                        if (isGenerating) {
                            binding.initialView.isVisible = false
                            binding.generatedMealPlanRecyclerView.isVisible = false
                            binding.btnMenu.isVisible = false
                            binding.nextButton.isEnabled = false
                        } else {
                            binding.btnMenu.isVisible = mealViewModel.currentMealPlan.value.isNotEmpty()
                            binding.nextButton.isEnabled = true
                        }
                    }
                }

                launch {
                    mealViewModel.currentMealPlan.collect { planItems ->
                        val isGenerating = mealViewModel.isGenerating.value == true
                        if (!isGenerating) {
                            val hasPlan = planItems.isNotEmpty()
                            binding.initialView.isVisible = !hasPlan
                            binding.generatedMealPlanRecyclerView.isVisible = hasPlan
                            binding.btnMenu.isVisible = hasPlan
                            mealAdapter.submitList(planItems)
                        }
                    }
                }

                launch {
                    mealViewModel.isPlanExpired.observe(viewLifecycleOwner) { isExpired ->
                        val hasPlan = mealViewModel.currentMealPlan.value.isNotEmpty()
                        binding.doneButton.isVisible = hasPlan && isExpired
                        binding.nextButton.isVisible = !hasPlan || isExpired
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

    private fun updateDateViews() {
        val today = LocalDate.now()
        val endDate = today.plusDays(6)
        val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val dayNameFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())

        binding.startDateText.text = today.format(monthDayFormatter)
        binding.endDayName.text = endDate.format(dayNameFormatter)
        binding.endDateText.text = endDate.format(monthDayFormatter)
    }
}
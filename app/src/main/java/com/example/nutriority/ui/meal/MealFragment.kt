package com.example.nutriority.ui.meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.FragmentMealBinding
import com.example.nutriority.ui.NavigationViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class MealFragment : Fragment() {

    private var _binding: FragmentMealBinding? = null
    private val binding get() = _binding!!

    private val mealViewModel: MealViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val mealAdapter by lazy {
        GeneratedMealPlanAdapter { meal ->
            val json = Gson().toJson(meal)
            navigationViewModel.navigateToMealDetail(json)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        updateDateViews()
        setupClickListeners()
        
        // IMMEDIATE CHECK: If data is already in ViewModel, show it now
        val currentPlan = mealViewModel.mealPlan.value
        if (!currentPlan.isNullOrEmpty() && currentPlan.any { it.isNotEmpty() }) {
            updateMealPlanUI(currentPlan)
        } else {
            // Ensure generate screen is visible if no plan
            binding.initialView.isVisible = true
            binding.generatedMealPlanRecyclerView.isVisible = false
        }
        
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.generatedMealPlanRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mealAdapter
        }
    }

    private fun setupClickListeners() {
        binding.nextButton.setOnClickListener {
            mealViewModel.generateMealPlan()
        }

        binding.doneButton.setOnClickListener {
            mealViewModel.completeMealPlan()
        }
    }

    private fun updateMealPlanUI(weeklyPlan: List<List<Meal>>) {
        val hasPlan = weeklyPlan.isNotEmpty() && weeklyPlan.any { it.isNotEmpty() }
        
        // FIX: Toggle visibility immediately based on plan presence
        binding.initialView.isVisible = !hasPlan
        binding.generatedMealPlanRecyclerView.isVisible = hasPlan

        if (hasPlan) {
            val mealListItems = weeklyPlan.mapIndexed { index, dailyMeals ->
                val dayLabel = mealViewModel.getDayLabel(index)
                listOf(MealListItem.HeaderItem(dayLabel)) + dailyMeals.map { meal ->
                    MealListItem.MealItem(meal)
                }
            }.flatten()
            mealAdapter.submitList(mealListItems)
        } else {
            mealAdapter.submitList(emptyList())
        }
        
        val isExpired = mealViewModel.isPlanExpired.value ?: false
        binding.doneButton.isVisible = hasPlan && isExpired
        binding.nextButton.isVisible = !hasPlan || isExpired
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mealViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                        binding.loadingProgressBar.isVisible = isLoading
                        if (isLoading) {
                            binding.initialView.isVisible = false
                            binding.generatedMealPlanRecyclerView.isVisible = false
                        }
                    }
                }

                launch {
                    mealViewModel.mealPlan.observe(viewLifecycleOwner) { weeklyPlan ->
                        if (weeklyPlan != null) updateMealPlanUI(weeklyPlan)
                    }
                }

                launch {
                    mealViewModel.isPlanExpired.observe(viewLifecycleOwner) { isExpired ->
                        val hasPlan = mealViewModel.mealPlan.value?.any { it.isNotEmpty() } == true
                        binding.doneButton.isVisible = hasPlan && isExpired
                        binding.nextButton.isVisible = !hasPlan || isExpired
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

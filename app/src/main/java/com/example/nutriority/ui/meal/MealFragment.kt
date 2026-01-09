package com.example.nutriority.ui.meal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.FragmentMealBinding
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class MealFragment : Fragment() {

    private var _binding: FragmentMealBinding? = null
    private val binding get() = _binding!!

    private val mealViewModel: MealViewModel by viewModels()
    private lateinit var mealAdapter: GeneratedMealPlanAdapter

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
        observeViewModel()
    }

    private fun setupRecyclerView() {
        mealAdapter = GeneratedMealPlanAdapter { meal ->
            val intent = Intent(requireContext(), MealDetailActivity::class.java)
            intent.putExtra("meal_json", Gson().toJson(meal))
            startActivity(intent)
        }
        
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

    private fun observeViewModel() {
        mealViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingProgressBar.isVisible = isLoading
            if (isLoading) {
                // Hide everything when loading
                binding.initialView.isVisible = false
                binding.generatedMealPlanRecyclerView.isVisible = false
                binding.doneButton.isVisible = false
            }
        }

        mealViewModel.mealPlan.observe(viewLifecycleOwner) { weeklyPlan ->
            val hasPlan = weeklyPlan.any { it.isNotEmpty() }

            binding.initialView.isVisible = !hasPlan && mealViewModel.isLoading.value == false
            binding.generatedMealPlanRecyclerView.isVisible = hasPlan

            if (hasPlan) {
                val mealListItems = weeklyPlan.mapIndexed { index, dailyMeals ->
                    val dayLabel = mealViewModel.getDayLabel(index)
                    val isToday = dayLabel.startsWith("Today")

                    listOf(MealListItem.HeaderItem(dayLabel)) + dailyMeals.map { meal ->
                        MealListItem.MealItem(meal, isToday)
                    }
                }.flatten()
                mealAdapter.submitList(mealListItems)
            } else {
                // Clear the adapter when there is no plan
                mealAdapter.submitList(emptyList())
            }
        }

        mealViewModel.isPlanExpired.observe(viewLifecycleOwner) { isExpired ->
            val hasPlan = mealViewModel.mealPlan.value?.any { it.isNotEmpty() } == true
            binding.doneButton.isVisible = hasPlan && isExpired
            // Hide the generate button if a plan exists and is not expired
            binding.nextButton.isVisible = !hasPlan || isExpired
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

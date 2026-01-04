package com.example.nutriority.ui.meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.FragmentMealBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class MealFragment : Fragment() {

    private var _binding: FragmentMealBinding? = null
    private val binding get() = _binding!!

    private val mealViewModel: MealViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDateViews()

        binding.generatedMealPlanRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.nextButton.setOnClickListener {
            mealViewModel.generateMealPlan()
        }

        mealViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingProgressBar.isVisible = isLoading
            // When loading starts, hide both the initial view and the results
            if (isLoading) {
                binding.initialView.isVisible = false
                binding.generatedMealPlanRecyclerView.isVisible = false
            }
        }

        mealViewModel.mealPlan.observe(viewLifecycleOwner) { weeklyPlan ->
            // This observer runs after the data is loaded/generated and isLoading is false.
            // The progress bar is already hidden by the isLoading observer.
            val hasPlan = weeklyPlan?.any { it.isNotEmpty() } == true

            if (hasPlan) {
                binding.initialView.isVisible = false
                binding.generatedMealPlanRecyclerView.isVisible = true

                val mealData = mutableListOf<Any>()
                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                val weekdaySdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                val calendar = Calendar.getInstance()

                weeklyPlan.forEachIndexed { index, dailyMeals ->
                    // Reset calendar to today and add the offset for the current day
                    val dayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, index) }
                    val dateHeader = when (index) {
                        0 -> "Today, ${sdf.format(dayCalendar.time)}"
                        1 -> "Tomorrow, ${sdf.format(dayCalendar.time)}"
                        else -> weekdaySdf.format(dayCalendar.time)
                    }
                    mealData.add(dateHeader)
                    mealData.addAll(dailyMeals)
                }

                val adapter = GeneratedMealPlanAdapter(mealData)
                binding.generatedMealPlanRecyclerView.adapter = adapter
            } else {
                binding.initialView.isVisible = true
                binding.generatedMealPlanRecyclerView.isVisible = false
            }
        }
    }

    private fun updateDateViews() {
        val sdfMonthDay = SimpleDateFormat("MMM d", Locale.getDefault())
        val sdfDayName = SimpleDateFormat("EEEE", Locale.getDefault())

        // Set start date
        val startCalendar = Calendar.getInstance()
        binding.startDateText.text = sdfMonthDay.format(startCalendar.time)

        // Set end date
        val endCalendar = startCalendar.clone() as Calendar
        endCalendar.add(Calendar.DAY_OF_YEAR, 6)
        binding.endDayName.text = sdfDayName.format(endCalendar.time)
        binding.endDateText.text = sdfMonthDay.format(endCalendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
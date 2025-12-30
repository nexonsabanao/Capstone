package com.example.nutriority.ui.meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        binding.generatedMealPlanRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.nextButton.setOnClickListener {
            // Hide the initial view and show the generated meal plan
            binding.initialView.visibility = View.GONE
            binding.generatedMealPlanRecyclerView.visibility = View.VISIBLE

            mealViewModel.generateMealPlan()
        }

        mealViewModel.mealPlan.observe(viewLifecycleOwner) { weeklyPlan ->
            val mealData = mutableListOf<Any>()
            if (weeklyPlan.isNotEmpty()) {
                val calendar = Calendar.getInstance()
                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                val weekdaySdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

                weeklyPlan.forEachIndexed { index, dailyMeals ->
                    val dateHeader = when (index) {
                        0 -> "Today, ${sdf.format(calendar.time)}"
                        1 -> "Tomorrow, ${sdf.format(calendar.time)}"
                        else -> weekdaySdf.format(calendar.time)
                    }
                    mealData.add(dateHeader)
                    mealData.addAll(dailyMeals)

                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Always set the adapter, even if the meal list is empty.
            val adapter = GeneratedMealPlanAdapter(mealData)
            binding.generatedMealPlanRecyclerView.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

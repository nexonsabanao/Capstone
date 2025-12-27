package com.example.nutriority.ui.meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nutriority.databinding.FragmentMealBinding
import dagger.hilt.android.AndroidEntryPoint

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

        binding.nextButton.setOnClickListener {
            // Hide the initial view and show the generated meal plan
            binding.initialView.visibility = View.GONE
            binding.generatedMealPlanRecyclerView.visibility = View.VISIBLE

            mealViewModel.allMeals.observe(viewLifecycleOwner) { meals ->
                if (meals.isNotEmpty()) {
                    val mealData = mutableListOf<Any>()
                    mealData.add("Today, Dec 26")
                    mealData.addAll(meals.shuffled().take(3))
                    mealData.add("Tomorrow, Dec 27")
                    mealData.addAll(meals.shuffled().take(3))

                    val adapter = GeneratedMealPlanAdapter(mealData)
                    binding.generatedMealPlanRecyclerView.adapter = adapter
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

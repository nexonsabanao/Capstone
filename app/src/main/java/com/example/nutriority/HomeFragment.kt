package com.example.nutriority

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.nutriority.models.Adapters.ArticleAdapter
import com.example.nutriority.models.Adapters.MealAdapter
import com.example.nutriority.models.Adapters.WorkoutAdapter
import com.example.nutriority.databinding.FragmentHomeBinding
import com.example.nutriority.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()

    // Declare adapters
    private lateinit var mealAdapter: MealAdapter
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var articleAdapter: ArticleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // --- CHANGE 1: Initialize ListAdapters without any data ---
        mealAdapter = MealAdapter()
        workoutAdapter = WorkoutAdapter()
        articleAdapter = ArticleAdapter()

        binding.mealsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mealAdapter
        }

        val workoutSnapHelper = PagerSnapHelper()
        binding.workoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = workoutAdapter
            workoutSnapHelper.attachToRecyclerView(this)
        }

        binding.articlesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = articleAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.allMeals.collect { meals ->
                        // --- CHANGE 2: Use submitList() instead of updateData() ---
                        mealAdapter.submitList(meals)
                    }
                }

                launch {
                    homeViewModel.allWorkouts.collect { workouts ->
                        // --- CHANGE 2: Use submitList() instead of updateData() ---
                        workoutAdapter.submitList(workouts)
                    }
                }

                launch {
                    homeViewModel.allArticles.collect { articles ->
                        articleAdapter.submitList(articles)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

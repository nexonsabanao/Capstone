package com.example.nutriority.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentHomeBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.MealAdapter
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.adapter.ArticleAdapter
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.relex.circleindicator.CircleIndicator2

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val mealAdapter by lazy {
        MealAdapter { meal ->
            val json = Gson().toJson(meal)
            navigationViewModel.navigateToMealDetail(json)
        }
    }

    private val workoutAdapter by lazy {
        WorkoutAdapter { workout ->
            navigationViewModel.navigateToWorkoutDetail(workout.id)
        }
    }

    private val articleAdapter by lazy {
        ArticleAdapter { article ->
            val json = Gson().toJson(article)
            navigationViewModel.navigateToArticleDetail(json)
        }
    }

    private lateinit var indicator: CircleIndicator2
    private val workoutSnapHelper = PagerSnapHelper()

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
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.sevenDaysWorkoutCard.btnStart.setOnClickListener {
            navigationViewModel.setTab(4)
        }

        binding.mealPlanCard.btnViewPlan.setOnClickListener {
            navigationViewModel.setTab(2)
        }
    }

    private fun setupRecyclerViews() {
        // Instant data injection if available
        val meals = homeViewModel.allMeals.value
        if (meals.isNotEmpty()) {
            mealAdapter.submitList(meals)
            binding.mealsRecyclerView.visibility = View.VISIBLE
            binding.mealsProgressBar.visibility = View.GONE
        }
        
        binding.mealsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            if (adapter != mealAdapter) adapter = mealAdapter
        }

        val workouts = homeViewModel.allWorkouts.value
        if (workouts.isNotEmpty()) {
            workoutAdapter.submitList(workouts)
            binding.workoutsRecyclerView.visibility = View.VISIBLE
            binding.workoutsIndicator.visibility = View.VISIBLE
            binding.workoutsProgressBar.visibility = View.GONE
        }

        binding.workoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            if (adapter != workoutAdapter) {
                adapter = workoutAdapter
                workoutSnapHelper.attachToRecyclerView(this)
            }
        }

        indicator = binding.workoutsIndicator
        indicator.attachToRecyclerView(binding.workoutsRecyclerView, workoutSnapHelper)
        
        try {
            workoutAdapter.registerAdapterDataObserver(indicator.adapterDataObserver)
        } catch (e: Exception) {}

        val articles = homeViewModel.allArticles.value
        if (articles.isNotEmpty()) {
            articleAdapter.submitList(articles)
            binding.articlesRecyclerView.visibility = View.VISIBLE
            binding.articlesProgressBar.visibility = View.GONE
        }

        binding.articlesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            if (adapter != articleAdapter) adapter = articleAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // OPTIMIZATION: Only collect data when the fragment is actually RESUMED (on screen)
            // This prevents background fragments from using CPU power.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    homeViewModel.allMeals.collect { meals ->
                        if (meals.isNotEmpty()) {
                            mealAdapter.submitList(meals)
                            binding.mealsRecyclerView.visibility = View.VISIBLE
                            binding.mealsProgressBar.visibility = View.GONE
                        }
                    }
                }

                launch {
                    homeViewModel.allWorkouts.collect { workouts ->
                        if (workouts.isNotEmpty()) {
                            workoutAdapter.submitList(workouts)
                            binding.workoutsRecyclerView.visibility = View.VISIBLE
                            binding.workoutsIndicator.visibility = View.VISIBLE
                            binding.workoutsProgressBar.visibility = View.GONE
                        }
                    }
                }

                launch {
                    homeViewModel.allArticles.collect { articles ->
                        if (articles.isNotEmpty()) {
                            articleAdapter.submitList(articles)
                            binding.articlesRecyclerView.visibility = View.VISIBLE
                            binding.articlesProgressBar.visibility = View.GONE
                        }
                    }
                }

                launch {
                    homeViewModel.calorieGoal.collect { calorieGoal ->
                        binding.mealPlanCard.tvCalories.text = calorieGoal
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            workoutAdapter.unregisterAdapterDataObserver(indicator.adapterDataObserver)
        } catch (e: Exception) {}
        _binding = null
    }
}

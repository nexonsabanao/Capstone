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
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.databinding.FragmentHomeBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.MealAdapter
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.adapter.ArticleAdapter
import com.example.nutriority.ui.workout.WorkoutDetailViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val workoutViewModel: WorkoutDetailViewModel by activityViewModels()

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

    private val workoutSnapHelper = PagerSnapHelper()
    private var isFirstWorkoutLoad = true

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

    private fun setupRecyclerViews() {
        // Meals
        binding.mealsRecyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mealAdapter
        }

        // Workouts
        binding.workoutsRecyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null // Essential to prevent flickering
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = workoutAdapter

            // Attach SnapHelper and Indicator once
            if (onFlingListener == null) {
                workoutSnapHelper.attachToRecyclerView(this)
                binding.workoutsIndicator.attachToRecyclerView(this, workoutSnapHelper)
            }
        }

        // Articles
        binding.articlesRecyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = articleAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Ongoing Workout Logic
                launch {
                    workoutViewModel.isWorkoutActive.collect { isActive ->
                        binding.ongoingWorkoutCard.visibility = if (isActive) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    workoutViewModel.completedExercisesCount.collect { completed ->
                        workoutViewModel.workout.value?.let { workout ->
                            if (workout.workout.id == workoutViewModel.activeWorkoutId.value) {
                                val total = workout.exerciseAssignments.size
                                val progress = if (total > 0) (completed.toFloat() / total.toFloat()) * 100 else 0f
                                binding.ongoingProgress.progress = progress
                                binding.tvOngoingSubtitle.text = "$completed from $total exercises done"
                            }
                        }
                    }
                }

                // Meals Logic
                launch {
                    homeViewModel.allMeals.collect { meals ->
                        if (meals.isNotEmpty()) {
                            mealAdapter.submitList(meals)
                            binding.mealsRecyclerView.visibility = View.VISIBLE
                            binding.mealsProgressBar.visibility = View.GONE
                        }
                    }
                }

                // Workouts Logic (Fix for Flicker and Indicator)
                launch {
                    homeViewModel.allWorkouts.collect { workouts ->
                        if (workouts.isNotEmpty()) {
                            workoutAdapter.submitList(workouts) {
                                // Logic inside this block runs AFTER DiffUtil finishes calculating
                                if (isFirstWorkoutLoad) {
                                    binding.workoutsRecyclerView.scrollToPosition(0)
                                    isFirstWorkoutLoad = false
                                }

                                // Update Indicator visibility and count
                                if (workouts.size > 1) {
                                    binding.workoutsIndicator.visibility = View.VISIBLE
                                    // Re-attaching can sometimes fix indicator not appearing after list submission
                                    binding.workoutsIndicator.createIndicators(workouts.size, 0)
                                } else {
                                    binding.workoutsIndicator.visibility = View.GONE
                                }
                            }
                            binding.workoutsRecyclerView.visibility = View.VISIBLE
                            binding.workoutsProgressBar.visibility = View.GONE
                        }
                    }
                }

                // Articles Logic
                launch {
                    homeViewModel.allArticles.collect { articles ->
                        if (articles.isNotEmpty()) {
                            articleAdapter.submitList(articles)
                            binding.articlesRecyclerView.visibility = View.VISIBLE
                            binding.articlesProgressBar.visibility = View.GONE
                        }
                    }
                }

                // Calories Logic
                launch {
                    homeViewModel.calorieGoal.collect { calorieGoal ->
                        binding.mealPlanCard.tvCalories.text = calorieGoal
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.sevenDaysWorkoutCard.btnStart.setOnClickListener {
            navigationViewModel.setTab(4)
        }

        binding.mealPlanCard.btnViewPlan.setOnClickListener {
            navigationViewModel.setTab(2)
        }

        binding.btnResumeOngoing.setOnClickListener {
            val activeId = workoutViewModel.activeWorkoutId.value
            if (activeId != -1) navigationViewModel.navigateToWorkoutDetail(activeId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

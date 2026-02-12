package com.example.nutriority.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.nutriority.databinding.FragmentHomeBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.MealAdapter
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.adapter.ArticleAdapter
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.workout.WorkoutDetailViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseBindingFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val workoutViewModel: WorkoutDetailViewModel by activityViewModels()

    private val mealAdapter by lazy {
        MealAdapter { meal ->
            navigationViewModel.navigateToMealDetail(Gson().toJson(meal))
        }
    }

    private val workoutAdapter by lazy {
        WorkoutAdapter { workout ->
            navigationViewModel.navigateToWorkoutDetail(workout.id)
        }
    }

    private val articleAdapter by lazy {
        ArticleAdapter { article ->
            navigationViewModel.navigateToArticleDetail(Gson().toJson(article))
        }
    }

    private val workoutSnapHelper = PagerSnapHelper()
    private var isFirstWorkoutLoad = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        binding.mealsRecyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mealAdapter
        }

        binding.workoutsRecyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null 
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = workoutAdapter

            if (onFlingListener == null) {
                workoutSnapHelper.attachToRecyclerView(this)
                binding.workoutsIndicator.attachToRecyclerView(this, workoutSnapHelper)
            }
        }

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
                // Ongoing Workout Visibility
                launch {
                    workoutViewModel.isWorkoutActive.collect { isActive ->
                        binding.ongoingWorkoutCard.visibility = if (isActive) View.VISIBLE else View.GONE
                    }
                }

                // FIXED Progress Bar Logic: 
                // Observe the completion count and the dedicated active workout detail together.
                // This ensures the HOME progress bar stays accurate even when browsing other details.
                launch {
                    combine(
                        workoutViewModel.completedExercisesCount,
                        workoutViewModel.activeWorkoutDetail
                    ) { completed, activeDetail ->
                        completed to activeDetail
                    }.collect { (completed, detail) ->
                        if (detail != null) {
                            val total = detail.exerciseAssignments.size
                            if (total > 0) {
                                val progress = (completed.toFloat() / total) * 100
                                binding.ongoingProgress.progress = progress
                                binding.tvOngoingSubtitle.text = "$completed from $total exercises done"
                            }
                        }
                    }
                }

                // Meals
                launch {
                    homeViewModel.allMeals.collect { meals ->
                        if (meals.isNotEmpty()) {
                            mealAdapter.submitList(meals)
                            binding.mealsRecyclerView.visibility = View.VISIBLE
                            binding.mealsProgressBar.visibility = View.GONE
                        }
                    }
                }

                // Workouts
                launch {
                    homeViewModel.allWorkouts.collect { workouts ->
                        if (workouts.isNotEmpty()) {
                            workoutAdapter.submitList(workouts) {
                                if (isFirstWorkoutLoad) {
                                    binding.workoutsRecyclerView.scrollToPosition(0)
                                    isFirstWorkoutLoad = false
                                }
                                binding.workoutsIndicator.visibility = if (workouts.size > 1) View.VISIBLE else View.GONE
                                if (workouts.size > 1) binding.workoutsIndicator.createIndicators(workouts.size, 0)
                            }
                            binding.workoutsRecyclerView.visibility = View.VISIBLE
                            binding.workoutsProgressBar.visibility = View.GONE
                        }
                    }
                }

                // Articles
                launch {
                    homeViewModel.allArticles.collect { articles ->
                        if (articles.isNotEmpty()) {
                            articleAdapter.submitList(articles)
                            binding.articlesRecyclerView.visibility = View.VISIBLE
                            binding.articlesProgressBar.visibility = View.GONE
                        }
                    }
                }

                // Calories
                launch {
                    homeViewModel.calorieGoal.collect { goal ->
                        binding.mealPlanCard.tvCalories.text = goal
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.sevenDaysWorkoutCard.btnStart.setOnClickListener { navigationViewModel.setTab(4) }
        binding.mealPlanCard.btnViewPlan.setOnClickListener { navigationViewModel.setTab(2) }
        binding.btnResumeOngoing.setOnClickListener {
            val activeId = workoutViewModel.activeWorkoutId.value
            if (activeId != -1) {
                // Determine if we need to pass the active detail back to navigation for seamless resume
                val activeDetail = workoutViewModel.activeWorkoutDetail.value
                val session = if (activeDetail?.workout?.category == "Personalized") {
                    // Logic to reconstruct/pass session if needed, for now we rely on the resolver in detail
                    null 
                } else null
                
                navigationViewModel.navigateToWorkoutDetail(activeId, session)
            }
        }
    }
}

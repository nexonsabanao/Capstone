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
import me.relex.circleindicator.CircleIndicator2

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
        binding.mealsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mealAdapter
        }

        binding.workoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = workoutAdapter
            if (onFlingListener == null) {
                workoutSnapHelper.attachToRecyclerView(this)
            }
        }

        binding.workoutsIndicator.attachToRecyclerView(binding.workoutsRecyclerView, workoutSnapHelper)

        binding.articlesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = articleAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    workoutViewModel.isWorkoutActive.collect { isActive ->
                        binding.ongoingWorkoutCard.visibility = if (isActive) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    workoutViewModel.completedExercisesCount.collect { completed ->
                        val workout = workoutViewModel.workout.value ?: return@collect
                        if (workout.workout.id == workoutViewModel.activeWorkoutId.value) {
                            val total = workout.exerciseAssignments.size
                            val progress = if (total > 0) (completed.toFloat() / total.toFloat()) * 100 else 0f
                            binding.ongoingProgress.progress = progress
                            binding.tvOngoingSubtitle.text = "$completed from $total exercises done"
                        }
                    }
                }

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
                    // RESTORED: Observe SMART list (allWorkouts) instead of raw database (unfilteredWorkouts)
                    homeViewModel.allWorkouts.collect { workouts ->
                        if (workouts.isNotEmpty()) {
                            workoutAdapter.submitList(workouts) {
                                binding.workoutsIndicator.attachToRecyclerView(binding.workoutsRecyclerView, workoutSnapHelper)
                                binding.workoutsIndicator.visibility = View.VISIBLE
                            }
                            binding.workoutsRecyclerView.visibility = View.VISIBLE
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

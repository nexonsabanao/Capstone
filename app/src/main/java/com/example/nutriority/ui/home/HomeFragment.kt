package com.example.nutriority.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.nutriority.MainActivity
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentHomeBinding
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

    private val homeViewModel: HomeViewModel by viewModels()

    private lateinit var mealAdapter: MealAdapter
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var indicator: CircleIndicator2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.sevenDaysWorkoutCard.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_personalizedWorkoutFragment)
        }

        binding.mealPlanCard.btnViewPlan.setOnClickListener {
            // Navigate using the bottom nav controller in MainActivity
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation_view)?.selectedItemId = R.id.navigation_meal
        }
    }

    private fun setupRecyclerViews() {
        mealAdapter = MealAdapter { meal ->
            val bundle = Bundle().apply {
                putString("meal_json", Gson().toJson(meal))
            }
            findNavController().navigate(R.id.action_navigation_home_to_mealDetailFragment, bundle)
        }
        
        workoutAdapter = WorkoutAdapter { workout ->
            val bundle = Bundle().apply {
                putInt("workout_id", workout.id)
            }
            findNavController().navigate(R.id.action_navigation_home_to_workoutDetailFragment, bundle)
        }
        
        articleAdapter = ArticleAdapter { article ->
            val bundle = Bundle().apply {
                putString("article_json", Gson().toJson(article))
            }
            findNavController().navigate(R.id.action_navigation_home_to_articleDetailFragment, bundle)
        }

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

        indicator = binding.workoutsIndicator
        indicator.attachToRecyclerView(binding.workoutsRecyclerView, workoutSnapHelper)

        workoutAdapter.registerAdapterDataObserver(indicator.adapterDataObserver)

        binding.articlesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = articleAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        binding.mealsRecyclerView.adapter = null
        binding.workoutsRecyclerView.adapter = null
        binding.articlesRecyclerView.adapter = null
        workoutAdapter.unregisterAdapterDataObserver(indicator.adapterDataObserver)
        _binding = null
    }
}

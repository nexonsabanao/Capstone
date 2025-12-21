package com.example.nutriority.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.nutriority.databinding.FragmentHomeBinding
import com.example.nutriority.ui.adapter.MealAdapter
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.adapter.ArticleAdapter
import com.example.nutriority.ui.workout.PersonalizedWorkoutActivity
import com.example.nutriority.ui.workout.WorkoutDetailActivity
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
            val intent = Intent(requireActivity(), PersonalizedWorkoutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerViews() {
        mealAdapter = MealAdapter()
        workoutAdapter = WorkoutAdapter { workout ->
            val intent = Intent(requireActivity(), WorkoutDetailActivity::class.java)
            intent.putExtra("workout_id", workout.id)
            startActivity(intent)
        }
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
                        mealAdapter.submitList(meals)
                    }
                }

                launch {
                    homeViewModel.allWorkouts.collect { workouts ->
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
        binding.mealsRecyclerView.adapter = null
        binding.workoutsRecyclerView.adapter = null
        binding.articlesRecyclerView.adapter = null
        workoutAdapter.unregisterAdapterDataObserver(indicator.adapterDataObserver)
        _binding = null
    }
}

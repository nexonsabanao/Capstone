package com.example.nutriority.ui.workout

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.ActivityWorkoutDetailBinding
import com.example.nutriority.ui.adapter.ExerciseAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutDetailBinding
    private val viewModel: WorkoutDetailViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val workoutId = intent.getIntExtra("workout_id", -1)
        if (workoutId != -1) {
            viewModel.getWorkoutById(workoutId)
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter { exercise ->
            val intent = Intent(this, ExerciseDetailActivity::class.java)
            intent.putExtra("exercise_id", exercise.id)
            startActivity(intent)
        }
        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@WorkoutDetailActivity)
            adapter = exerciseAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.workout.collect { workoutWithExercises ->
                workoutWithExercises?.let {
                    supportActionBar?.title = it.workout.name
                    exerciseAdapter.submitList(it.exercises)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

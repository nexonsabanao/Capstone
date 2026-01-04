package com.example.nutriority.ui.workout

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.databinding.ActivityWorkoutDetailBinding
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.workout.SimpleItemTouchHelperCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutDetailBinding
    private val viewModel: WorkoutDetailViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT)

        val workoutId = intent.getIntExtra("workout_id", -1)
        if (workoutId != -1) {
            viewModel.getWorkoutById(workoutId)
        }

        setupRecyclerView()
        observeViewModel()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter(
            onItemClick = { exercise ->
                val intent = Intent(this, ExerciseDetailActivity::class.java)
                intent.putExtra("exercise_id", exercise.id)
                startActivity(intent)
            },
            onListUpdated = { exercises ->
                viewModel.updateExercises(exercises)
            },
            onDragStart = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )

        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@WorkoutDetailActivity)
            adapter = exerciseAdapter
        }

        val callback = SimpleItemTouchHelperCallback(exerciseAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.exercisesRecyclerView)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.workout.collect { workoutWithExercises ->
                workoutWithExercises?.let { workout ->
                    binding.collapsingToolbar.title = workout.workout.name
                    supportActionBar?.title = ""
                    binding.workoutTitle.text = workout.workout.name
                    binding.workoutDuration.text = workout.workout.duration
                    val filteredExercises = workout.exercises.filter {
                        !it.category.equals("Warm-up", ignoreCase = true) && !it.category.equals("Cool-down", ignoreCase = true)
                    }
                    binding.workoutExerciseCount.text = filteredExercises.size.toString()
                    exerciseAdapter.submitList(filteredExercises.sortedBy { exercise -> exercise.order })
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

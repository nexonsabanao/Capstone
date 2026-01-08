package com.example.nutriority.ui.workout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.data.model.ExerciseSet
import com.example.nutriority.databinding.ActivityExerciseDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseDetailBinding
    private val viewModel: ExerciseDetailViewModel by viewModels()
    private lateinit var exerciseSetAdapter: ExerciseSetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val exerciseId = intent.getIntExtra("exercise_id", -1)
        val exercisePosition = intent.getIntExtra("exercise_position", -1)
        val totalExercises = intent.getIntExtra("total_exercises", -1)

        if (exerciseId != -1) {
            viewModel.getExerciseById(exerciseId)
        }

        if (exercisePosition != -1 && totalExercises != -1) {
            binding.exerciseCountText.text = "$exercisePosition/$totalExercises"
        }

        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        exerciseSetAdapter = ExerciseSetAdapter(
            onRepClick = { position ->
                // Handle editing reps for the set at this position
            },
            onDeleteClick = { position ->
                // Handle deleting the set at this position
            }
        )
        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExerciseDetailActivity)
            adapter = exerciseSetAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.exercise.collect { exercise ->
                exercise?.let { it ->
                    binding.exerciseTitle.text = it.name
                    // Load the image using Glide
                    // Glide.with(this@ExerciseDetailActivity).load(it.image).into(binding.imgExercise)

                    // Create a list of ExerciseSet objects
                    val setsList = (1..it.sets).map { _ ->
                        ExerciseSet(reps = it.reps, isActive = false)
                    }
                    // Set the first set to be active
                    if (setsList.isNotEmpty()) {
                        setsList.first().isActive = true
                    }
                    exerciseSetAdapter.submitList(setsList)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.btnCheck.setOnClickListener {
            // Handle moving to the next set
        }

        binding.btnAutoLog.setOnClickListener {
            if (binding.btnAutoLog.text.toString().contains("OFF")) {
                binding.btnAutoLog.text = "Auto Log : ON"
            } else {
                binding.btnAutoLog.text = "Auto Log : OFF"
            }
        }

        binding.btnRest.setOnClickListener {
            if (binding.btnRest.text.toString().contains("ON")) {
                binding.btnRest.text = "Rest : OFF"
            } else {
                binding.btnRest.text = "Rest : ON"
            }
        }

        binding.btnLogSet.setOnClickListener {
            // Handle logging the set
        }
    }
}

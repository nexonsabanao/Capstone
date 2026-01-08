package com.example.nutriority.ui.workout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
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
    private lateinit var addSetAdapter: AddSetAdapter
    private var currentSets = listOf<ExerciseSet>()

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
                val mutableList = currentSets.toMutableList()
                if (mutableList.size > 1) {
                    mutableList.removeAt(position)
                    updateAndSubmitList(mutableList)
                }
            }
        )

        addSetAdapter = AddSetAdapter {
            val mutableList = currentSets.toMutableList()
            val newSet = ExerciseSet(reps = 8) // Default reps, you can change this
            mutableList.add(newSet)
            updateAndSubmitList(mutableList)
        }

        val concatAdapter = ConcatAdapter(exerciseSetAdapter, addSetAdapter)

        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExerciseDetailActivity)
            adapter = concatAdapter
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
                    val initialSets = (1..it.sets).map { _ ->
                        ExerciseSet(reps = it.reps)
                    }
                    updateAndSubmitList(initialSets)
                }
            }
        }
    }

    private fun updateAndSubmitList(updatedSets: List<ExerciseSet>) {
        // Create a new list with updated set numbers and active state
        currentSets = updatedSets.mapIndexed { index, set ->
            set.copy(
                setNumber = index + 1,
                isActive = index == 0 // Always make the first set active
            )
        }
        exerciseSetAdapter.submitList(currentSets)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the adapter to help prevent memory leaks
        binding.setsRecyclerView.adapter = null
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

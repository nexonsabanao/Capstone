package com.example.nutriority.ui.workout

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.model.ExerciseSet
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.databinding.ActivityExerciseDetailBinding
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import java.util.Date

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
                showEditRepsDialog(position)
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
            val newSet = ExerciseSet(reps = currentSets.lastOrNull()?.reps ?: 8)
            mutableList.add(newSet)
            updateAndSubmitList(mutableList)
        }

        val concatAdapter = ConcatAdapter(exerciseSetAdapter, addSetAdapter)

        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExerciseDetailActivity)
            adapter = concatAdapter
        }
    }

    private fun showEditRepsDialog(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_reps, null)
        val repsInput = dialogView.findViewById<EditText>(R.id.edit_reps_input)
        val btnOk = dialogView.findViewById<MaterialButton>(R.id.btn_ok)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        repsInput.setText(currentSets[position].reps.toString())

        btnOk.setOnClickListener {
            val newReps = repsInput.text.toString().toIntOrNull()
            if (newReps != null) {
                val mutableList = currentSets.toMutableList()
                val updatedSet = mutableList[position].copy(reps = newReps)
                mutableList[position] = updatedSet
                updateAndSubmitList(mutableList)
            }
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.exercise.collect { exercise ->
                exercise?.let { ex ->
                    binding.exerciseTitle.text = ex.name
                    // Load the image using Glide
                    // Glide.with(this@ExerciseDetailActivity).load(it.image).into(binding.imgExercise)

                    // Correctly parse reps string or fall back to sets count
                    val repsList = ex.reps.split(",").mapNotNull { it.trim().toIntOrNull() }
                    val initialSets = if (repsList.size == ex.sets) {
                        // The saved data is consistent, use it directly
                        repsList.map { ExerciseSet(reps = it) }
                    } else {
                        // Data is from pre-population or inconsistent, create sets from scratch
                        val defaultRep = repsList.firstOrNull() ?: 8
                        List(ex.sets) { ExerciseSet(reps = defaultRep) }
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

        // Save the changes to the database
        viewModel.exercise.value?.let { currentExercise ->
            val updatedRepsString = currentSets.joinToString(", ") { it.reps.toString() }
            val updatedExercise = currentExercise.copy(
                sets = currentSets.size, // Correctly update the sets count
                reps = updatedRepsString // And the reps string
            )
            viewModel.updateExercise(updatedExercise)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the adapter to help prevent memory leaks
        binding.setsRecyclerView.adapter = null
        
        // Log the workout
        viewModel.exercise.value?.let { exercise ->
            val repsString = currentSets.joinToString(", ") { it.reps.toString() }
            val log = WorkoutLog(
                workoutId = exercise.workoutId,
                date = Date(),
                reps = repsString
            )
            viewModel.logWorkout(log)
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

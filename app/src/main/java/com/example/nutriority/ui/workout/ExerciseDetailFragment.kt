package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.model.ExerciseSet
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.databinding.FragmentExerciseDetailBinding
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class ExerciseDetailFragment : Fragment() {

    private var _binding: FragmentExerciseDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExerciseDetailViewModel by viewModels()
    private lateinit var exerciseSetAdapter: ExerciseSetAdapter
    private lateinit var addSetAdapter: AddSetAdapter
    private var currentSets = listOf<ExerciseSet>()
    
    private var isProcessing = false
    private var currentDialog: AlertDialog? = null
    private var isInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exerciseId = arguments?.getInt("exercise_id", -1) ?: -1
        val exercisePosition = arguments?.getInt("exercise_position", -1) ?: -1
        val totalExercises = arguments?.getInt("total_exercises", -1) ?: -1

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
                if (!isProcessing && position >= 0 && position < currentSets.size) {
                    isProcessing = true
                    val mutableList = currentSets.toMutableList()
                    if (mutableList.size > 1) {
                        mutableList.removeAt(position)
                        updateAndSubmitList(mutableList)
                    } else {
                        Toast.makeText(requireContext(), "Workout must have at least one set", Toast.LENGTH_SHORT).show()
                    }
                    binding.root.postDelayed({ isProcessing = false }, 150)
                }
            }
        )

        addSetAdapter = AddSetAdapter {
            if (!isProcessing) {
                isProcessing = true
                val lastSet = currentSets.lastOrNull()
                val exercise = viewModel.exercise.value
                
                val isDuration = if (exercise != null) {
                    exercise.category.contains("Warm-up", ignoreCase = true) || 
                    exercise.category.contains("Cool-down", ignoreCase = true) ||
                    (exercise.duration.isNotBlank() && (exercise.duration.contains("s") || exercise.duration.contains(":")))
                } else {
                    lastSet?.isDuration ?: false
                }

                val newValue = lastSet?.value ?: if (isDuration) 30 else 8
                val mutableList = currentSets.toMutableList()
                val newSet = ExerciseSet(value = newValue, isDuration = isDuration)
                mutableList.add(newSet)
                updateAndSubmitList(mutableList)
                
                binding.root.postDelayed({ isProcessing = false }, 150)
            }
        }

        val concatAdapter = ConcatAdapter(exerciseSetAdapter, addSetAdapter)

        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
        }
    }

    private fun showEditRepsDialog(position: Int) {
        currentDialog?.dismiss()

        if (position < 0 || position >= currentSets.size) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_reps, null)
        val repsInput = dialogView.findViewById<EditText>(R.id.edit_reps_input)
        val btnOk = dialogView.findViewById<MaterialButton>(R.id.btn_ok)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)

        currentDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set transparent background to allow rounded corners to show correctly
        currentDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        repsInput.setText(currentSets[position].value.toString())

        btnOk.setOnClickListener {
            val newValue = repsInput.text.toString().toIntOrNull()
            if (newValue != null) {
                val mutableList = currentSets.toMutableList()
                if (position >= 0 && position < mutableList.size) {
                    val updatedSet = mutableList[position].copy(value = newValue)
                    mutableList[position] = updatedSet
                    updateAndSubmitList(mutableList)
                }
            }
            currentDialog?.dismiss()
        }

        btnCancel.setOnClickListener {
            currentDialog?.dismiss()
        }

        currentDialog?.show()
    }

    private fun showAboutExerciseDialog() {
        viewModel.exercise.value?.let { exercise ->
            AboutExerciseBottomSheet.newInstance(exercise)
                .show(childFragmentManager, "AboutExerciseBottomSheet")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.exercise.collect { exercise ->
                if (exercise != null && !isInitialized) {
                    isInitialized = true
                    binding.exerciseTitle.text = exercise.name

                    val isWarmupCooldown = exercise.category.contains("Warm-up", ignoreCase = true) || 
                                         exercise.category.contains("Cool-down", ignoreCase = true)
                    
                    val durationStr = "${exercise.duration} ${exercise.reps}".lowercase()
                    val isDuration = isWarmupCooldown || durationStr.contains("s") || durationStr.contains(":")
                    
                    val parsedValue = if (isDuration) {
                        val sourceStr = if (exercise.duration.any { it.isDigit() }) exercise.duration else exercise.reps
                        if (sourceStr.contains(":")) {
                            val parts = sourceStr.split(":")
                            val mins = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            val secs = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            (mins * 60) + secs
                        } else {
                            sourceStr.filter { it.isDigit() }.toIntOrNull() ?: 30
                        }
                    } else {
                        exercise.reps.split(",").firstOrNull()?.trim()?.filter { it.isDigit() }?.toIntOrNull() ?: 8
                    }

                    val savedValues = exercise.reps.split(",").mapNotNull { it.trim().filter { c -> c.isDigit() }.toIntOrNull() }
                    val setsCount = if (isWarmupCooldown && exercise.sets <= 0) 1 else exercise.sets

                    val initialSets = if (savedValues.size == setsCount && savedValues.isNotEmpty()) {
                        savedValues.map { ExerciseSet(value = it, isDuration = isDuration) }
                    } else {
                        List(setsCount) { ExerciseSet(value = parsedValue, isDuration = isDuration) }
                    }
                    
                    updateAndSubmitList(initialSets)
                }
            }
        }
    }

    private fun updateAndSubmitList(updatedSets: List<ExerciseSet>) {
        currentSets = updatedSets.mapIndexed { index, set ->
            set.copy(setNumber = index + 1)
        }
        
        val hasActive = currentSets.any { it.isActive }
        if (!hasActive && currentSets.isNotEmpty()) {
            currentSets[0].isActive = true
        }
        
        exerciseSetAdapter.submitList(currentSets)

        viewModel.exercise.value?.let { currentExercise ->
            val updatedValueString = currentSets.joinToString(", ") { it.value.toString() }
            val updatedExercise = currentExercise.copy(
                sets = currentSets.size,
                reps = updatedValueString
            )
            viewModel.updateExercise(updatedExercise)
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAboutExercise.setOnClickListener {
            showAboutExerciseDialog()
        }

        binding.btnCheck.setOnClickListener {
            val activeIndex = currentSets.indexOfFirst { it.isActive }
            if (activeIndex != -1 && activeIndex < currentSets.size - 1) {
                val mutableList = currentSets.toMutableList()
                mutableList[activeIndex] = mutableList[activeIndex].copy(isActive = false)
                mutableList[activeIndex + 1] = mutableList[activeIndex + 1].copy(isActive = true)
                currentSets = mutableList
                exerciseSetAdapter.submitList(currentSets)
                binding.btnLogSet.text = "Log set ${activeIndex + 2}"
            } else {
                Toast.makeText(requireContext(), "All sets complete! Tap Log to finish.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAutoLog.setOnClickListener {
            val isOff = binding.btnAutoLog.text.toString().contains("OFF")
            binding.btnAutoLog.text = if (isOff) "Auto Log : ON" else "Auto Log : OFF"
        }

        binding.btnRest.setOnClickListener {
            val isOn = binding.btnRest.text.toString().contains("ON")
            binding.btnRest.text = if (isOn) "Rest : OFF" else "Rest : ON"
        }

        binding.btnLogSet.setOnClickListener {
            viewModel.exercise.value?.let { exercise ->
                val valueString = currentSets.joinToString(", ") { it.value.toString() }
                val log = WorkoutLog(
                    workoutId = exercise.workoutId ?: 0,
                    date = Date(),
                    reps = valueString
                )
                viewModel.logWorkout(log)
                Toast.makeText(requireContext(), "Workout logged successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.setsRecyclerView.adapter = null
        _binding = null
    }
}

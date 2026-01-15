package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.model.ExerciseSet
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.databinding.FragmentExerciseDetailBinding
import com.example.nutriority.ui.NavigationViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class ExerciseDetailFragment : Fragment() {

    private var _binding: FragmentExerciseDetailBinding? = null
    private val binding get() = _binding!!
    
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: ExerciseDetailViewModel by activityViewModels()
    
    private var isInitialized = false

    private val exerciseSetAdapter by lazy {
        ExerciseSetAdapter(
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
                    }
                    binding.root.postDelayed({ isProcessing = false }, 150)
                }
            }
        )
    }
    
    private val addSetAdapter by lazy {
        AddSetAdapter {
            if (!isProcessing) {
                isProcessing = true
                val lastSet = currentSets.lastOrNull()
                val isDuration = lastSet?.isDuration ?: false

                val newValue = lastSet?.value ?: if (isDuration) 30 else 10
                val mutableList = currentSets.toMutableList()
                val newSet = ExerciseSet(value = newValue, isDuration = isDuration)
                mutableList.add(newSet)
                updateAndSubmitList(mutableList)
                
                binding.root.postDelayed({ isProcessing = false }, 150)
            }
        }
    }
    
    private var currentSets = listOf<ExerciseSet>()
    private var isProcessing = false
    private var currentDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeNavigationData()
        observeViewModel()
        setupClickListeners()
    }

    private fun observeNavigationData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    navigationViewModel.selectedWorkoutId,
                    navigationViewModel.selectedExerciseId
                ) { wId, eId -> wId to eId }
                .collect { (workoutId, exerciseId) ->
                    if (workoutId != -1 && exerciseId.isNotBlank()) {
                        isInitialized = false
                        viewModel.getExerciseById(workoutId, exerciseId)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    navigationViewModel.exercisePosition,
                    navigationViewModel.totalExercises
                ) { pos, total -> pos to total }
                    .collect { (pos, total) ->
                        if (pos != -1 && total != -1) {
                            binding.exerciseCountText.text = "$pos/$total"
                        }
                    }
            }
        }
    }

    private fun setupRecyclerView() {
        val concatAdapter = ConcatAdapter(exerciseSetAdapter, addSetAdapter)
        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
            itemAnimator = null
        }
    }

    private fun showEditRepsDialog(position: Int) {
        currentDialog?.dismiss()
        if (position < 0 || position >= currentSets.size) return

        val exerciseSet = currentSets[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_reps, null)
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.dialog_subtitle)
        val layoutInput = dialogView.findViewById<TextInputLayout>(R.id.edit_reps_layout)
        val repsInput = dialogView.findViewById<EditText>(R.id.edit_reps_input)
        val btnOk = dialogView.findViewById<MaterialButton>(R.id.btn_ok)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)

        if (exerciseSet.isDuration) {
            tvTitle.text = "Enter Seconds"
            tvSubtitle.text = "How many seconds did you complete?"
            layoutInput.hint = "Number of seconds"
            repsInput.hint = "Number of seconds"
        } else {
            tvTitle.text = "Enter Repetitions"
            tvSubtitle.text = "How many reps did you complete?"
            layoutInput.hint = "Number of reps"
            repsInput.hint = "Number of reps"
        }

        currentDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        currentDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        repsInput.setText(exerciseSet.value.toString())

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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exerciseWithDetail.collect { detail ->
                    if (detail != null && !isInitialized) {
                        isInitialized = true
                        val exercise = detail.exercise
                        val assignment = detail.assignment
                        
                        binding.exerciseTitle.text = exercise.name
                        
                        val isDuration = assignment.category.contains("Warm-up", true) || 
                                         assignment.category.contains("Cool-down", true) ||
                                         assignment.duration.isNotBlank()

                        val setsCount = assignment.sets.coerceAtLeast(1)
                        val initialValue = if (isDuration) {
                            assignment.duration.filter { it.isDigit() }.toIntOrNull() ?: 30
                        } else {
                            assignment.reps.split(",").firstOrNull()?.trim()?.filter { it.isDigit() }?.toIntOrNull() ?: 10
                        }

                        val initialSets = List(setsCount) { 
                            ExerciseSet(value = initialValue, isDuration = isDuration) 
                        }
                        updateAndSubmitList(initialSets)
                    }
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
        
        val activeSet = currentSets.find { it.isActive }
        if (activeSet != null) {
            val unit = if (activeSet.isDuration) "SEC" else "REPS"
            binding.btnLogSet.text = "LOG SET ${activeSet.setNumber} (${activeSet.value} $unit)"
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            navigationViewModel.goBack()
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
                updateAndSubmitList(mutableList)
            } else {
                Toast.makeText(requireContext(), "All sets complete! Tap Log to finish.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogSet.setOnClickListener {
            viewModel.exerciseWithDetail.value?.let { detail ->
                val valueString = currentSets.joinToString(", ") { it.value.toString() }
                val log = WorkoutLog(
                    workoutId = detail.assignment.workoutId,
                    date = Date(),
                    reps = valueString
                )
                viewModel.logWorkout(log)
                navigationViewModel.goBack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

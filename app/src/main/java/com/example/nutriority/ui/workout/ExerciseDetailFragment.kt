package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class ExerciseDetailFragment : Fragment() {

    private var _binding: FragmentExerciseDetailBinding? = null
    private val binding get() = _binding!!
    
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: ExerciseDetailViewModel by activityViewModels()
    private val workoutViewModel: WorkoutDetailViewModel by activityViewModels()
    
    private var isInitialized = false
    private var isRestOn = true 
    private var isAutoLogOn = false
    private var autoLogTimeSeconds = 30L

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

    private fun showAutoLogBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_auto_log_bottom_sheet, null)
        dialog.setContentView(view)

        val switch = view.findViewById<MaterialSwitch>(R.id.switchAutoLog)
        switch.isChecked = isAutoLogOn
        switch.setOnCheckedChangeListener { _, isChecked ->
            isAutoLogOn = isChecked
            binding.btnAutoLog.text = if (isChecked) "Auto Log : ON" else "Auto Log : OFF"
            if (isChecked) {
                startAutoLogTimerIfNeeded()
            } else {
                workoutViewModel.stopAutoLogTimer()
            }
        }

        view.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.timeChipGroup)
        
        val checkId = when(autoLogTimeSeconds) {
            15L -> R.id.chip_15s
            30L -> R.id.chip_30s
            45L -> R.id.chip_45s
            60L -> R.id.chip_1m
            else -> R.id.chip_recd
        }
        chipGroup.check(checkId)

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            autoLogTimeSeconds = when (checkedIds.firstOrNull()) {
                R.id.chip_15s -> 15L
                R.id.chip_30s -> 30L
                R.id.chip_45s -> 45L
                R.id.chip_1m -> 60L
                else -> 30L
            }
            if (isAutoLogOn) startAutoLogTimerIfNeeded()
        }

        dialog.show()
    }

    private fun startAutoLogTimerIfNeeded() {
        if (!isAutoLogOn) return
        
        val activeSet = currentSets.find { it.isActive && !it.isCompleted } ?: return
        val time = if (activeSet.isDuration) activeSet.value.toLong() else autoLogTimeSeconds
        
        workoutViewModel.startAutoLogTimer(time) {
            logSetAndAdvance()
        }
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
                            ExerciseSet(value = initialValue, isDuration = isDuration, isCompleted = assignment.isCompleted) 
                        }
                        updateAndSubmitList(initialSets)
                        
                        if (isAutoLogOn) startAutoLogTimerIfNeeded()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.isResting.collect { isResting: Boolean ->
                    binding.restTimerBar.visibility = if (isResting) View.VISIBLE else View.GONE
                    binding.bottomBar.visibility = if (isResting) View.GONE else View.VISIBLE
                    
                    if (!isResting && isAutoLogOn) {
                        startAutoLogTimerIfNeeded()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.restTimeRemaining.collect { seconds: Long ->
                    val mins = seconds / 60
                    val secs = seconds % 60
                    binding.tvRestTimer.text = String.format("%02d:%02ds", mins, secs)
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
            val nextActiveIndex = currentSets.indexOfFirst { !it.isCompleted }
            if (nextActiveIndex != -1) {
                currentSets[nextActiveIndex].isActive = true
            }
        }
        
        exerciseSetAdapter.submitList(currentSets)
        
        val allCompleted = currentSets.all { it.isCompleted }
        val activeSet = currentSets.find { it.isActive }
        
        if (allCompleted) {
            val isLastExercise = navigationViewModel.exercisePosition.value == navigationViewModel.totalExercises.value
            binding.btnLogSet.text = if (isLastExercise) "FINISH WORKOUT" else "NEXT EXERCISE"
            binding.btnLogSet.setIconResource(if (isLastExercise) R.drawable.ic_check_circle else R.drawable.ic_play_arrow)
            binding.btnLogSet.setBackgroundColor(resources.getColor(R.color.green, null))
            binding.btnCheck.visibility = View.GONE
        } else if (activeSet != null) {
            val unit = if (activeSet.isDuration) "SEC" else "REPS"
            binding.btnLogSet.text = "LOG SET ${activeSet.setNumber} (${activeSet.value} $unit)"
            binding.btnLogSet.setIconResource(android.R.drawable.ic_input_get)
            binding.btnLogSet.setBackgroundColor(resources.getColor(R.color.primary_dark, null))
            binding.btnCheck.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            workoutViewModel.stopRestTimer() // Explicitly stop rest timer on back
            navigationViewModel.goBack()
        }

        binding.btnAboutExercise.setOnClickListener {
            showAboutExerciseDialog()
        }

        binding.btnRest.setOnClickListener {
            isRestOn = !isRestOn
            binding.btnRest.text = if (isRestOn) "Rest : ON" else "Rest : OFF"
        }

        binding.btnAutoLog.setOnClickListener {
            showAutoLogBottomSheet()
        }

        binding.btnCheck.setOnClickListener {
            ensureWorkoutStarted()
            completeAllSets()
        }

        binding.btnLogSet.setOnClickListener {
            ensureWorkoutStarted()
            val allCompleted = currentSets.all { it.isCompleted }
            if (allCompleted) {
                finishOrNext()
            } else {
                logSetAndAdvance()
            }
        }

        // Rest Bar Listeners
        binding.btnCloseRest.setOnClickListener { workoutViewModel.stopRestTimer() }
        binding.btnStopRest.setOnClickListener { workoutViewModel.stopRestTimer() }
        binding.btnMinus5.setOnClickListener { workoutViewModel.adjustRestTime(-5) }
        binding.btnPlus5.setOnClickListener { workoutViewModel.adjustRestTime(5) }
    }

    private fun ensureWorkoutStarted() {
        if (!workoutViewModel.isWorkoutActive.value) {
            val workoutId = navigationViewModel.selectedWorkoutId.value
            if (workoutId != -1) {
                workoutViewModel.startWorkout(workoutId)
            }
        }
    }

    private fun completeAllSets() {
        workoutViewModel.stopAutoLogTimer() 
        val updatedList = currentSets.map { it.copy(isActive = false, isCompleted = true) }
        updateAndSubmitList(updatedList)
        
        viewModel.exerciseWithDetail.value?.let { detail ->
            workoutViewModel.updateExerciseCompletion(
                detail.assignment.workoutId,
                detail.assignment.exerciseId,
                detail.assignment.category,
                true
            )
        }
    }

    private fun finishOrNext() {
        workoutViewModel.stopAutoLogTimer()
        workoutViewModel.stopRestTimer() // Ensure rest timer is stopped
        
        viewLifecycleOwner.lifecycleScope.launch {
            val detail = viewModel.exerciseWithDetail.value ?: return@launch
            val workoutWithExercises = workoutViewModel.workout.first() ?: return@launch
            
            workoutViewModel.updateExerciseCompletion(
                detail.assignment.workoutId,
                detail.assignment.exerciseId,
                detail.assignment.category,
                true
            )

            val valueString = currentSets.joinToString(", ") { it.value.toString() }
            val log = WorkoutLog(
                workoutId = detail.assignment.workoutId,
                date = Date(),
                reps = valueString
            )
            viewModel.logWorkout(log)

            val currentPos = navigationViewModel.exercisePosition.value
            val total = navigationViewModel.totalExercises.value

            if (currentPos < total) {
                val includeAll = workoutWithExercises.workout.includeWarmupCooldown
                val assignments = workoutWithExercises.exerciseAssignments.sortedBy { it.assignment.order }
                val visibleAssignments = if (includeAll) assignments else assignments.filter { it.assignment.category.equals("Exercise", ignoreCase = true) }

                if (currentPos < visibleAssignments.size) {
                    val nextAssignment = visibleAssignments[currentPos]
                    navigationViewModel.navigateToExerciseDetail(
                        nextAssignment.assignment.workoutId,
                        nextAssignment.assignment.exerciseId,
                        currentPos + 1,
                        visibleAssignments.size
                    )
                } else {
                    workoutViewModel.finishWorkout()
                    navigationViewModel.navigateToWorkoutComplete()
                }
            } else {
                workoutViewModel.finishWorkout()
                navigationViewModel.navigateToWorkoutComplete()
            }
        }
    }

    private fun logSetAndAdvance() {
        val activeIndex = currentSets.indexOfFirst { it.isActive }
        if (activeIndex != -1) {
            val mutableList = currentSets.toMutableList()
            mutableList[activeIndex] = mutableList[activeIndex].copy(isActive = false, isCompleted = true)
            
            val isFinalSet = activeIndex == currentSets.size - 1
            
            if (!isFinalSet) {
                mutableList[activeIndex + 1] = mutableList[activeIndex + 1].copy(isActive = true)
                updateAndSubmitList(mutableList)
                
                if (isRestOn) {
                    val restTime = viewModel.exerciseWithDetail.value?.assignment?.rest?.filter { it.isDigit() }?.toLongOrNull() ?: 60L
                    workoutViewModel.startRestTimer(restTime)
                } else if (isAutoLogOn) {
                    startAutoLogTimerIfNeeded()
                }
            } else {
                updateAndSubmitList(mutableList)
                
                if (isRestOn) {
                    val restTime = viewModel.exerciseWithDetail.value?.assignment?.rest?.filter { it.isDigit() }?.toLongOrNull() ?: 60L
                    workoutViewModel.startRestTimer(restTime)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        workoutViewModel.stopRestTimer() // Cleanup rest timer when fragment is destroyed
        workoutViewModel.stopAutoLogTimer()
        _binding = null
    }
}

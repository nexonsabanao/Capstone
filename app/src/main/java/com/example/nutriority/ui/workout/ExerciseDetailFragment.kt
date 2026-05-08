package com.example.nutriority.ui.workout

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nutriority.R
import com.example.nutriority.data.model.ExerciseSet
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.databinding.FragmentExerciseDetailBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
@AndroidEntryPoint
class ExerciseDetailFragment : BaseBindingFragment<FragmentExerciseDetailBinding>(FragmentExerciseDetailBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: ExerciseDetailViewModel by activityViewModels()
    private val workoutViewModel: WorkoutDetailViewModel by activityViewModels()

    private var isRestOn = true
    private var isAutoLogOn = false
    private var autoLogTimeSeconds = 30L
    
    private val exerciseSetAdapter by lazy {
        ExerciseSetAdapter(
            onRepClick = { position -> showEditRepsDialog(position) },
            onDeleteClick = { position -> handleDeleteSet(position) }
        )
    }

    private val addSetAdapter by lazy {
        AddSetAdapter { handleAddSet() }
    }

    private var currentSets = listOf<ExerciseSet>()
    private var isProcessing = false
    private var currentDialog: AlertDialog? = null
    private var lastObservedAssignmentId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeNavigationData()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ConcatAdapter(exerciseSetAdapter, addSetAdapter)
            itemAnimator = null
        }
    }

    private fun handleAddSet() {
        if (isProcessing) return
        isProcessing = true
        val lastSet = currentSets.lastOrNull()
        val isDuration = lastSet?.isDuration ?: false
        val newValue = lastSet?.value ?: 10

        val detail = viewModel.exerciseWithDetail.value
        val nextSetNumber = currentSets.size + 1
        val stableId = if (detail != null) {
            "${detail.assignment.workoutId}_${detail.assignment.exerciseId}_${detail.assignment.category}_$nextSetNumber"
        } else {
            java.util.UUID.randomUUID().toString()
        }

        val mutableList = currentSets.toMutableList().apply {
            add(ExerciseSet(id = stableId, setNumber = nextSetNumber, value = newValue, isDuration = isDuration))
        }
        updateAndSubmitList(mutableList)
        saveChangesToDatabase(mutableList)
        binding.root.postDelayed({ isProcessing = false }, 150)
    }

    private fun handleDeleteSet(position: Int) {
        if (isProcessing || position < 0 || position >= currentSets.size) return
        isProcessing = true
        val mutableList = currentSets.toMutableList()
        if (mutableList.size > 1) {
            mutableList.removeAt(position)
            updateAndSubmitList(mutableList)
            saveChangesToDatabase(mutableList)
        }
        binding.root.postDelayed({ isProcessing = false }, 150)
    }

    private fun showEditRepsDialog(position: Int) {
        currentDialog?.dismiss()
        if (position < 0 || position >= currentSets.size) return

        val exerciseSet = currentSets[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_reps, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val layoutInput = dialogView.findViewById<TextInputLayout>(R.id.edit_reps_layout)
        val repsInput = dialogView.findViewById<EditText>(R.id.edit_reps_input)
        val btnOk = dialogView.findViewById<MaterialButton>(R.id.btn_ok)

        tvTitle.text = if (exerciseSet.isDuration) "Enter Seconds" else "Enter Repetitions"
        layoutInput.hint = if (exerciseSet.isDuration) "Number of seconds" else "Number of reps"
        repsInput.setText(exerciseSet.value.toString())

        currentDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

        btnOk.setOnClickListener {
            val newValue = repsInput.text.toString().toIntOrNull()
            if (newValue != null) {
                val mutableList = currentSets.toMutableList()
                mutableList[position] = mutableList[position].copy(value = newValue)
                updateAndSubmitList(mutableList)
                saveChangesToDatabase(mutableList)
            }
            currentDialog?.dismiss()
        }
        dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener { currentDialog?.dismiss() }
        currentDialog?.show()
    }

    private fun saveChangesToDatabase(updatedSets: List<ExerciseSet>) {
        val detail = viewModel.exerciseWithDetail.value ?: return
        val isDuration = updatedSets.firstOrNull()?.isDuration ?: false
        val valStr = updatedSets.joinToString(",") { it.value.toString() }

        val updated = detail.assignment.copy(
            sets = updatedSets.size,
            reps = if (isDuration) "1" else valStr,
            duration = if (isDuration) valStr else ""
        )
        viewLifecycleOwner.lifecycleScope.launch { workoutViewModel.updateWorkoutExercise(updated) }
    }

    private fun observeNavigationData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    navigationViewModel.workoutNavRequest,
                    navigationViewModel.selectedExerciseId,
                    navigationViewModel.selectedCategory
                ) { request, eId, cat -> Triple(request.workoutId, eId, cat) }
                    .collect { (wId, eId, cat) ->
                        if (wId != -1 && eId.isNotBlank()) {
                            lastObservedAssignmentId = null
                            viewModel.getExerciseById(wId, eId, cat)
                        }
                    }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(navigationViewModel.exercisePosition, navigationViewModel.totalExercises) { p, t -> p to t }
                    .collect { (pos, total) -> if (pos != -1 && total != -1) binding.exerciseCountText.text = getString(R.string.exercise_count_format, pos, total) }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.contentScrollView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
                    binding.bottomBarContainer.visibility = if (isLoading) View.GONE else View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exercise.collect { ex ->
                    if (ex != null) {
                        binding.exerciseTitle.text = ex.name
                        Glide.with(this@ExerciseDetailFragment).asGif().load(ex.gifUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL).into(binding.imgExercise)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exerciseWithDetail.collect { detail ->
                    if (detail != null) {
                        val assign = detail.assignment
                        val currentId = "${assign.workoutId}_${assign.exerciseId}_${assign.category}"

                        if (lastObservedAssignmentId != currentId) {
                            lastObservedAssignmentId = currentId
                            val isDur = assign.category.contains("warmup", true) || assign.category.contains("cooldown", true) || assign.duration.isNotBlank()

                            val rawValue = if (isDur) assign.duration else assign.reps
                            val vals = if (rawValue.contains(",")) {
                                rawValue.split(",")
                            } else if (rawValue.contains("-")) {
                                listOf(rawValue.split("-").first().trim())
                            } else {
                                listOf(rawValue.trim())
                            }

                            val initial = List(assign.sets.coerceAtLeast(1)) { i ->
                                val setNum = i + 1
                                val raw = vals.getOrNull(i)?.trim() ?: vals.firstOrNull()?.trim() ?: ""
                                val v = if (isDur) parseTimeToSeconds(raw).takeIf { it > 0 } ?: 60 else raw.filter { it.isDigit() }.toIntOrNull() ?: 10
                                ExerciseSet(
                                    id = "${assign.workoutId}_${assign.exerciseId}_${assign.category}_$setNum",
                                    setNumber = setNum,
                                    value = v,
                                    isDuration = isDur,
                                    isCompleted = assign.isCompleted
                                )
                            }
                            updateAndSubmitList(initial)
                            if (isAutoLogOn) startAutoLogTimerIfNeeded()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.isResting.collect { isResting ->
                    binding.restTimerBar.visibility = if (isResting) View.VISIBLE else View.GONE
                    binding.bottomBar.visibility = if (isResting) View.GONE else View.VISIBLE
                    
                    if (!isResting) {
                        if (currentSets.isNotEmpty() && currentSets.all { it.isCompleted }) {
                            val currentPos = navigationViewModel.exercisePosition.value
                            val total = navigationViewModel.totalExercises.value
                            if (currentPos != -1 && total != -1 && currentPos < total) {
                                finishOrNext()
                            }
                        } else if (isAutoLogOn) {
                            startAutoLogTimerIfNeeded()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.restTimeRemaining.collect { s ->
                    binding.tvRestTimer.text = String.format(Locale.getDefault(), "%02d:%02ds", s / 60, s % 60)
                }
            }
        }
    }

    private fun updateAndSubmitList(updated: List<ExerciseSet>) {
        if (_binding == null) return // Safety check to prevent crash if fragment is detached
        
        currentSets = updated.mapIndexed { i, s -> s.copy(setNumber = i + 1) }
        if (!currentSets.any { it.isActive } && currentSets.isNotEmpty()) {
            val next = currentSets.indexOfFirst { !it.isCompleted }
            if (next != -1) currentSets[next].isActive = true
        }
        exerciseSetAdapter.submitList(currentSets)

        val allDone = currentSets.all { it.isCompleted }
        val active = currentSets.find { it.isActive }
        val isLast = (navigationViewModel.exercisePosition.value) == (navigationViewModel.totalExercises.value)

        // Disable/Enable Auto-Log based on workout status
        if (allDone && isLast) {
            isAutoLogOn = false
            workoutViewModel.stopAutoLogTimer()
            binding.btnAutoLog.apply {
                text = "Auto-Log: OFF"
                isEnabled = false
                setOnClickListener(null)
            }
        } else {
            binding.btnAutoLog.apply {
                isEnabled = true
                text = if (isAutoLogOn) "Auto-Log: ON" else "Auto-Log: OFF"
                setOnClickListener { showAutoLogBottomSheet() }
            }
        }

        if (allDone) {
            binding.btnLogSet.apply {
                text = if (isLast) "FINISH WORKOUT" else "NEXT EXERCISE"
                setIconResource(if (isLast) R.drawable.ic_check_circle else R.drawable.ic_play_arrow)
                setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            }
            binding.btnCheck.visibility = View.GONE
        } else if (active != null) {
            binding.btnLogSet.apply {
                text = getString(R.string.log_set_format, active.setNumber, active.value, if (active.isDuration) "SEC" else "REPS")
                setIconResource(android.R.drawable.ic_input_get)
                setBackgroundColor(ContextCompat.getColor(context, R.color.primary_dark))
            }
            binding.btnCheck.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { workoutViewModel.stopRestTimer(); navigationViewModel.goBack() }
        binding.btnAboutExercise.setOnClickListener { viewModel.exercise.value?.let { AboutExerciseBottomSheet.newInstance(it).show(childFragmentManager, "AboutExerciseBottomSheet") } }
        binding.btnRest.setOnClickListener { isRestOn = !isRestOn; binding.btnRest.text = if (isRestOn) "Rest : ON" else "Rest : OFF" }
        // Note: btnAutoLog listener is now managed in updateAndSubmitList
        binding.btnCheck.setOnClickListener { ensureWorkoutStarted(); completeAllSets() }
        binding.btnLogSet.setOnClickListener {
            ensureWorkoutStarted()
            if (currentSets.all { it.isCompleted }) finishOrNext() else logSetAndAdvance()
        }
        binding.btnCloseRest.setOnClickListener { workoutViewModel.stopRestTimer() }
        binding.btnStopRest.setOnClickListener { workoutViewModel.stopRestTimer() }
        binding.btnMinus5.setOnClickListener { workoutViewModel.adjustRestTime(-5) }
        binding.btnPlus5.setOnClickListener { workoutViewModel.adjustRestTime(5) }
    }

    private fun ensureWorkoutStarted() {
        if (!workoutViewModel.isWorkoutActive.value) {
            val request = navigationViewModel.workoutNavRequest.value
            if (request.workoutId != -1) {
                workoutViewModel.startWorkout(request.workoutId, request.dayIndex)
            }
        }
    }

    private fun playSetCompleteSound() {
        try {
            val mp = MediaPlayer.create(requireContext(), R.raw.set_complete)
            mp.setVolume(1.0f, 1.0f) // Set to max relative volume
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveExercisePerformanceLog() {
        val detail = viewModel.exerciseWithDetail.value ?: return
        val log = WorkoutLog(
            workoutId = detail.assignment.workoutId,
            exerciseName = detail.exercise.name,
            date = Date(),
            reps = currentSets.joinToString(",") { it.value.toString() },
            weightKg = 0.0
        )
        viewModel.logWorkout(log)
    }

    private fun logSetAndAdvance() {
        val index = currentSets.indexOfFirst { it.isActive }
        if (index == -1) return

        // Stop current auto-log timer just in case it was triggered manually
        workoutViewModel.stopAutoLogTimer()

        playSetCompleteSound()

        val mutableSets = currentSets.toMutableList()
        val current = mutableSets[index]
        mutableSets[index] = current.copy(isCompleted = true, isActive = false)

        val nextIndex = index + 1
        if (nextIndex < mutableSets.size) {
            mutableSets[nextIndex] = mutableSets[nextIndex].copy(isActive = true)
        }
        
        if (isRestOn) {
            val currentPos = navigationViewModel.exercisePosition.value
            val total = navigationViewModel.totalExercises.value
            val isLastExercise = currentPos != -1 && total != -1 && currentPos >= total
            val isFinalSetOfWorkout = isLastExercise && nextIndex >= mutableSets.size
            
            if (!isFinalSetOfWorkout) {
                val detail = viewModel.exerciseWithDetail.value
                var restSecs = parseTimeToSeconds(detail?.assignment?.rest ?: "60s")
                if (restSecs <= 0) restSecs = 30 
                workoutViewModel.startRestTimer(restSecs.toLong())
            }
        }

        updateAndSubmitList(mutableSets)

        val detail = viewModel.exerciseWithDetail.value ?: return
        if (mutableSets.all { it.isCompleted }) {
            saveExercisePerformanceLog()
            workoutViewModel.updateExerciseCompletion(detail.assignment.workoutId, detail.assignment.exerciseId, detail.assignment.category, true)
            if (!isRestOn) {
                // BUG FIX: If rest is OFF, go to next exercise immediately
                finishOrNext()
            }
        } else if (!isRestOn && isAutoLogOn) {
            // BUG FIX: If rest is OFF but auto-log is ON, start timer for next set manually
            startAutoLogTimerIfNeeded()
        }
    }

    private fun completeAllSets() {
        playSetCompleteSound()

        val mutableSets = currentSets.map { it.copy(isCompleted = true, isActive = false) }
        updateAndSubmitList(mutableSets)
        val detail = viewModel.exerciseWithDetail.value ?: return
        
        saveExercisePerformanceLog()
        
        workoutViewModel.updateExerciseCompletion(detail.assignment.workoutId, detail.assignment.exerciseId, detail.assignment.category, true)
        
        if (isRestOn) {
            val currentPos = navigationViewModel.exercisePosition.value
            val total = navigationViewModel.totalExercises.value
            val isLastExercise = currentPos != -1 && total != -1 && currentPos >= total
            
            if (!isLastExercise) {
                var restSecs = parseTimeToSeconds(detail.assignment.rest)
                if (restSecs <= 0) restSecs = 30
                workoutViewModel.startRestTimer(restSecs.toLong())
            }
        } else {
            finishOrNext()
        }
    }

    private fun finishOrNext() {
        workoutViewModel.stopAutoLogTimer() // Stop any running auto-log timers before navigating
        val currentPos = navigationViewModel.exercisePosition.value
        val total = navigationViewModel.totalExercises.value
        val isLast = currentPos != -1 && total != -1 && currentPos >= total
        
        if (isLast) {
            workoutViewModel.finishWorkout()
            navigationViewModel.navigateToWorkoutComplete()
        } else {
            navigationViewModel.nextExercise()
        }
    }

    private fun showAutoLogBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_auto_log_bottom_sheet, binding.root as? ViewGroup, false)

        val switchAutoLog = view.findViewById<MaterialSwitch>(R.id.switchAutoLog)
        val timeChipGroup = view.findViewById<ChipGroup>(R.id.timeChipGroup)

        switchAutoLog.isChecked = isAutoLogOn
        val chipIdToCheck = when(autoLogTimeSeconds) {
            15L -> R.id.chip_15s
            30L -> R.id.chip_30s
            45L -> R.id.chip_45s
            60L -> R.id.chip_1m
            else -> R.id.chip_30s
        }
        timeChipGroup.check(chipIdToCheck)

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        
        switchAutoLog.setOnCheckedChangeListener { _, isChecked ->
            isAutoLogOn = isChecked
            binding.btnAutoLog.text = if (isAutoLogOn) "Auto-Log: ON" else "Auto-Log: OFF"
            if (isAutoLogOn) startAutoLogTimerIfNeeded()
        }

        timeChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            autoLogTimeSeconds = when(chip?.id) {
                R.id.chip_15s -> 15L
                R.id.chip_30s -> 30L
                R.id.chip_45s -> 45L
                R.id.chip_1m -> 60L
                else -> 30L
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun startAutoLogTimerIfNeeded() {
        if (!isAutoLogOn || workoutViewModel.isResting.value) return
        if (!currentSets.any { it.isActive }) return
        workoutViewModel.startAutoLogTimer(autoLogTimeSeconds) {
            // Safety check before performing UI updates from a background timer callback
            if (_binding != null && isAdded) {
                logSetAndAdvance()
            }
        }
    }

    private fun parseTimeToSeconds(timeStr: String): Int {
        val clean = timeStr.lowercase().trim()
        return when {
            clean.endsWith("s") -> clean.dropLast(1).trim().toIntOrNull() ?: 0
            clean.endsWith("min") -> (clean.replace("min", "").trim().toIntOrNull() ?: 0) * 60
            clean.endsWith("m") -> (clean.dropLast(1).trim().toIntOrNull() ?: 0) * 60
            clean.contains(":") -> {
                val parts = clean.split(":")
                val m = parts[0].toIntOrNull() ?: 0
                val s = parts.getOrNull(1)?.toIntOrNull() ?: 0
                m * 60 + s
            }
            else -> clean.toIntOrNull() ?: 0
        }
    }
}

package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.View
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
import com.example.nutriority.databinding.FragmentExerciseDetailBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.WorkoutItem
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class ExerciseDetailFragment : BaseBindingFragment<FragmentExerciseDetailBinding>(FragmentExerciseDetailBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: ExerciseDetailViewModel by activityViewModels()
    private val workoutViewModel: WorkoutDetailViewModel by activityViewModels()
    
    private var isInitialized = false
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
            setHasFixedSize(true)
        }
    }

    private fun handleAddSet() {
        if (isProcessing) return
        isProcessing = true
        val lastSet = currentSets.lastOrNull()
        val isDuration = lastSet?.isDuration ?: false
        val newValue = lastSet?.value ?: if (isDuration) 30 else 10
        val mutableList = currentSets.toMutableList().apply {
            add(ExerciseSet(value = newValue, isDuration = isDuration))
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
                combine(navigationViewModel.selectedWorkoutId, navigationViewModel.selectedExerciseId) { w, e -> w to e }
                .collect { (wId, eId) ->
                    if (wId != -1 && eId.isNotBlank()) {
                        isInitialized = false
                        viewModel.getExerciseById(wId, eId)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(navigationViewModel.exercisePosition, navigationViewModel.totalExercises) { p, t -> p to t }
                .collect { (pos, total) -> if (pos != -1 && total != -1) binding.exerciseCountText.text = "$pos/$total" }
            }
        }
    }

    private fun observeViewModel() {
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
                    if (detail != null && !isInitialized) {
                        isInitialized = true
                        val assign = detail.assignment
                        val isDur = assign.category.contains("warmup", true) || assign.category.contains("cooldown", true) || assign.duration.isNotBlank()
                        val vals = if (isDur) assign.duration.split(",") else assign.reps.split(",")
                        val initial = List(assign.sets.coerceAtLeast(1)) { i ->
                            val raw = vals.getOrNull(i)?.trim() ?: vals.firstOrNull()?.trim() ?: ""
                            val v = if (isDur) parseTimeToSeconds(raw).takeIf { it > 0 } ?: 60 else raw.filter { it.isDigit() }.toIntOrNull() ?: 10
                            ExerciseSet(value = v, isDuration = isDur, isCompleted = assign.isCompleted) 
                        }
                        updateAndSubmitList(initial)
                        if (isAutoLogOn) startAutoLogTimerIfNeeded()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.isResting.collect { isResting ->
                    binding.restTimerBar.visibility = if (isResting) View.VISIBLE else View.GONE
                    binding.bottomBar.visibility = if (isResting) View.GONE else View.VISIBLE
                    if (!isResting && isAutoLogOn) startAutoLogTimerIfNeeded()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.restTimeRemaining.collect { s ->
                    binding.tvRestTimer.text = String.format("%02d:%02ds", s / 60, s % 60)
                }
            }
        }
    }

    private fun updateAndSubmitList(updated: List<ExerciseSet>) {
        currentSets = updated.mapIndexed { i, s -> s.copy(setNumber = i + 1) }
        if (!currentSets.any { it.isActive } && currentSets.isNotEmpty()) {
            val next = currentSets.indexOfFirst { !it.isCompleted }
            if (next != -1) currentSets[next].isActive = true
        }
        exerciseSetAdapter.submitList(currentSets)
        
        val allDone = currentSets.all { it.isCompleted }
        val active = currentSets.find { it.isActive }
        
        if (allDone) {
            val isLast = navigationViewModel.exercisePosition.value == navigationViewModel.totalExercises.value
            binding.btnLogSet.apply {
                text = if (isLast) "FINISH WORKOUT" else "NEXT EXERCISE"
                setIconResource(if (isLast) R.drawable.ic_check_circle else R.drawable.ic_play_arrow)
                setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            }
            binding.btnCheck.visibility = View.GONE
        } else if (active != null) {
            binding.btnLogSet.apply {
                text = "LOG SET ${active.setNumber} (${active.value} ${if (active.isDuration) "SEC" else "REPS"})"
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
        binding.btnAutoLog.setOnClickListener { showAutoLogBottomSheet() }
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
            navigationViewModel.selectedWorkoutId.value.takeIf { it != -1 }?.let { workoutViewModel.startWorkout(it, navigationViewModel.selectedDayIndex.value) }
        }
    }

    private fun completeAllSets() {
        workoutViewModel.stopAutoLogTimer() 
        val updated = currentSets.map { it.copy(isActive = false, isCompleted = true) }
        updateAndSubmitList(updated)
        viewModel.exerciseWithDetail.value?.let { detail -> workoutViewModel.updateExerciseCompletion(detail.assignment.workoutId, detail.assignment.exerciseId, detail.assignment.category, true) }
    }

    private fun finishOrNext() {
        workoutViewModel.stopAutoLogTimer(); workoutViewModel.stopRestTimer()
        viewLifecycleOwner.lifecycleScope.launch {
            val detail = viewModel.exerciseWithDetail.value ?: return@launch
            val workout = workoutViewModel.workout.first() ?: return@launch
            workoutViewModel.updateExerciseCompletion(detail.assignment.workoutId, detail.assignment.exerciseId, detail.assignment.category, true)
            val pos = navigationViewModel.exercisePosition.value
            val total = navigationViewModel.totalExercises.value
            if (pos < total) {
                val assignments = workout.exerciseAssignments.sortedBy { it.assignment.order }.filter { workout.workout.includeWarmupCooldown || it.assignment.category.equals("Exercise", true) }
                if (pos < assignments.size) {
                    val next = assignments[pos]
                    navigationViewModel.navigateToExerciseDetail(next.assignment.workoutId, next.assignment.exerciseId, pos + 1, assignments.size)
                } else { workoutViewModel.finishWorkout(); navigationViewModel.navigateToWorkoutComplete() }
            } else { workoutViewModel.finishWorkout(); navigationViewModel.navigateToWorkoutComplete() }
        }
    }

    private fun logSetAndAdvance() {
        val activeIdx = currentSets.indexOfFirst { it.isActive }
        if (activeIdx != -1) {
            val mutable = currentSets.toMutableList()
            mutable[activeIdx] = mutable[activeIdx].copy(isActive = false, isCompleted = true)
            if (activeIdx < currentSets.size - 1) {
                mutable[activeIdx + 1] = mutable[activeIdx + 1].copy(isActive = true)
                updateAndSubmitList(mutable)
                if (isRestOn) {
                    val rest = viewModel.exerciseWithDetail.value?.assignment?.rest?.filter { it.isDigit() }?.toLongOrNull() ?: 60L
                    workoutViewModel.startRestTimer(rest)
                } else if (isAutoLogOn) startAutoLogTimerIfNeeded()
            } else updateAndSubmitList(mutable)
        }
    }

    private fun showAutoLogBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val v = layoutInflater.inflate(R.layout.layout_auto_log_bottom_sheet, null)
        dialog.setContentView(v)
        v.findViewById<MaterialSwitch>(R.id.switchAutoLog).apply {
            isChecked = isAutoLogOn
            setOnCheckedChangeListener { _, isChecked ->
                isAutoLogOn = isChecked
                binding.btnAutoLog.text = if (isChecked) "Auto Log : ON" else "Auto Log : OFF"
                if (isChecked) startAutoLogTimerIfNeeded() else workoutViewModel.stopAutoLogTimer()
            }
        }
        v.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun startAutoLogTimerIfNeeded() {
        if (!isAutoLogOn) return
        val active = currentSets.find { it.isActive && !it.isCompleted } ?: return
        workoutViewModel.startAutoLogTimer(if (active.isDuration) active.value.toLong() else autoLogTimeSeconds) { logSetAndAdvance() }
    }

    private fun parseTimeToSeconds(s: String): Int {
        val lower = s.lowercase().trim()
        val v = lower.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return 0
        return if (lower.contains("min") || (lower.contains("m") && !lower.contains("s"))) (v * 60).toInt() else v.toInt()
    }
}

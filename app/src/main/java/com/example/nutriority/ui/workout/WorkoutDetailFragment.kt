package com.example.nutriority.ui.workout

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.databinding.FragmentWorkoutDetailBinding
import com.example.nutriority.databinding.DialogEditWorkoutBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.SelectableExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.ImageUtil
import com.example.nutriority.ui.util.KeyboardUtil
import com.example.nutriority.ui.util.WorkoutUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class WorkoutDetailFragment : BaseBindingFragment<FragmentWorkoutDetailBinding>(FragmentWorkoutDetailBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var isSettingInitialState = false
    private var currentToast: Toast? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeNavigationData()
        observeViewModel()
        setupClickListeners()
        setupOnBackPressed()
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isWorkoutActive.value && viewModel.activeWorkoutId.value == viewModel.workout.value?.workout?.id) {
                    showEndWorkoutBottomSheet()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { navigationViewModel.goBack() }
        binding.tvToolbarTitle.alpha = 0f
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener

            val percentage = abs(verticalOffset).toFloat() / totalScrollRange
            val startFadeAt = 0.8f
            if (percentage > startFadeAt) {
                val alphaProgress = (percentage - startFadeAt) / (1f - startFadeAt)
                binding.toolbar.setBackgroundColor(Color.argb((alphaProgress * 255).toInt(), 255, 255, 255))
                binding.tvToolbarTitle.alpha = alphaProgress
            } else {
                binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
                binding.tvToolbarTitle.alpha = 0f
            }
        })
    }

    private fun showToast(message: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }

    private fun observeNavigationData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.workoutNavRequest.collect { request ->
                    if (request.workoutId != -1) {
                        viewModel.resolveWorkout(request.workoutId, request.session)
                        binding.nestedScrollView.scrollTo(0, 0)
                        binding.appBarLayout.setExpanded(true)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.addExerciseButton.setOnClickListener { showEditWorkoutDialog() }
        binding.switchIncludeWarmupCooldown.setOnCheckedChangeListener { _, isChecked ->
            if (isSettingInitialState) return@setOnCheckedChangeListener
            viewModel.updateWorkoutPreference(isChecked)
        }
        binding.startButton.setOnClickListener { handleStartAction() }
        binding.btnEndWorkout.setOnClickListener { showEndWorkoutBottomSheet() }
    }

    private fun handleStartAction() {
        val currentWorkout = viewModel.workout.value ?: return
        val activeWorkoutId = viewModel.activeWorkoutId.value
        
        if (viewModel.isWorkoutActive.value && activeWorkoutId != currentWorkout.workout.id) {
            showToast("Another workout is in progress!")
            return
        }
        
        navigationViewModel.setCurrentWorkoutData(currentWorkout)
        val navRequest = navigationViewModel.workoutNavRequest.value
        viewModel.startWorkout(currentWorkout.workout.id, navRequest.dayIndex)
        navigateToCurrentExercise()
    }

    private fun navigateToCurrentExercise() {
        val items = exerciseAdapter.currentList.filterIsInstance<WorkoutItem.ExerciseItem>()
        if (items.isNotEmpty()) {
            val first = items[0].detail.assignment
            navigationViewModel.navigateToExerciseDetail(
                workoutId = first.workoutId, 
                exerciseId = first.exerciseId, 
                category = first.category,
                position = 1, 
                total = items.size
            )
        }
    }

    private fun showEndWorkoutBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_end_workout_bottom_sheet, null)
        dialog.setContentView(view)

        val workout = viewModel.workout.value ?: return
        val include = workout.workout.includeWarmupCooldown
        val filtered = if (include) {
            workout.exerciseAssignments
        } else {
            workout.exerciseAssignments.filter { it.assignment.category.equals("Exercise", true) }
        }
        val total = filtered.size
        val done = filtered.count { it.assignment.isCompleted }
        val progress = if (total > 0) (done * 100) / total else 0

        view.findViewById<TextView>(R.id.tvSubtitle).text = "($done from $total completed - $progress%)"

        val resume = { viewModel.resumeWorkout(); dialog.dismiss() }
        view.findViewById<View>(R.id.btnClose).setOnClickListener { resume() }
        view.findViewById<MaterialButton>(R.id.btnResume).setOnClickListener { resume() }
        
        dialog.setOnCancelListener { viewModel.resumeWorkout() }
        view.findViewById<MaterialButton>(R.id.btnDiscard).setOnClickListener {
            viewModel.stopWorkout(false); dialog.dismiss()
            navigationViewModel.goBack()
        }
        view.findViewById<MaterialButton>(R.id.btnSaveFinish).setOnClickListener {
            viewModel.finishWorkout(); navigationViewModel.navigateToWorkoutComplete(); dialog.dismiss()
        }

        viewModel.pauseWorkout()
        dialog.show()
    }

    private fun showEditWorkoutDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
        val dialogBinding = DialogEditWorkoutBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val selectableAdapter = SelectableExerciseAdapter { _, _ -> }
        dialogBinding.rvSelectExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectableAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val workoutWithExercises = viewModel.workout.filterNotNull().first()
            val allExercisesList = viewModel.getAllExercises().first()
            val workoutName = workoutWithExercises.workout.name
            val isEditable = workoutWithExercises.workout.id > 25

            dialogBinding.workoutNameLayout.visibility = if (isEditable) View.VISIBLE else View.GONE
            dialogBinding.btnReset.visibility = if (isEditable) View.GONE else View.VISIBLE
            if (isEditable) dialogBinding.etWorkoutName.setText(workoutName)

            setupTargetChips(dialogBinding, allExercisesList, selectableAdapter)

            selectableAdapter.setData(allExercisesList, workoutWithExercises.exerciseAssignments.map { it.exercise.copy(category = it.assignment.category) })

            dialogBinding.etSearchExercises.doAfterTextChanged { query ->
                selectableAdapter.setSearchQuery(query?.toString() ?: "")
            }

            dialogBinding.etSearchExercises.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    KeyboardUtil.hideKeyboard(v)
                    v.clearFocus()
                    true
                } else false
            }

            dialogBinding.btnReset.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    dialogBinding.loadingProgress.visibility = View.VISIBLE
                    dialogBinding.contentLayout.visibility = View.GONE
                    
                    val originalAssignments = viewModel.getOriginalAssignments(workoutWithExercises.workout.id)
                    if (originalAssignments.isNotEmpty()) {
                        // Create a temporary list of exercises matching the original assignments
                        val originalExercises = originalAssignments.mapNotNull { assignment ->
                            allExercisesList.find { it.id == assignment.exerciseId }?.copy(category = assignment.category)
                        }
                        selectableAdapter.setData(allExercisesList, originalExercises)
                        showToast("Workout reset to default")
                    } else {
                        showToast("Could not fetch original workout")
                    }
                    
                    dialogBinding.loadingProgress.visibility = View.GONE
                    dialogBinding.contentLayout.visibility = View.VISIBLE
                }
            }

            dialogBinding.btnSave.setOnClickListener {
                val name = if (isEditable) dialogBinding.etWorkoutName.text.toString() else workoutName
                if (name.isBlank()) return@setOnClickListener
                val newAssignments = selectableAdapter.getSelectedExercises().mapIndexed { i, ex ->
                    val existing = workoutWithExercises.exerciseAssignments.find { it.assignment.exerciseId == ex.id && it.assignment.category == ex.category }
                    com.example.nutriority.data.model.WorkoutExercise(
                        workoutId = workoutWithExercises.workout.id,
                        exerciseId = ex.id,
                        category = ex.category.ifBlank { "Exercise" },
                        sets = existing?.assignment?.sets ?: if (ex.category == "Exercise") 3 else 1,
                        reps = existing?.assignment?.reps ?: if (ex.category == "Exercise") "10" else "1",
                        rest = existing?.assignment?.rest ?: if (ex.category == "Exercise") "60s" else "0s",
                        duration = existing?.assignment?.duration ?: if (ex.category == "Exercise") "" else "1 min",
                        order = i
                    )
                }
                viewModel.updateWorkout(workoutWithExercises.workout.copy(name = name), newAssignments)
                dialog.dismiss()
            }
            dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }
            dialogBinding.loadingProgress.visibility = View.GONE
            dialogBinding.contentLayout.visibility = View.VISIBLE
        }
        dialog.show()
    }

    private fun setupTargetChips(dialogBinding: DialogEditWorkoutBinding, all: List<com.example.nutriority.data.model.Exercise>, adapter: SelectableExerciseAdapter) {
        val targets = listOf("Abs", "Arms", "Back", "Chest", "Legs", "Shoulders", "Full Body")
        dialogBinding.targetMuscleChipGroup.removeAllViews()
        val allChip = LayoutInflater.from(requireContext()).inflate(R.layout.layout_filter_chip, dialogBinding.targetMuscleChipGroup, false) as Chip
        allChip.text = "All"; allChip.isChecked = true; allChip.id = View.generateViewId()
        dialogBinding.targetMuscleChipGroup.addView(allChip)

        targets.forEach { t ->
            val chip = LayoutInflater.from(requireContext()).inflate(R.layout.layout_filter_chip, dialogBinding.targetMuscleChipGroup, false) as Chip
            chip.text = t; chip.id = View.generateViewId()
            dialogBinding.targetMuscleChipGroup.addView(chip)
        }

        fun filter() {
            val cat = when(dialogBinding.categoryChipGroup.checkedChipId) {
                R.id.chip_warmup -> "warmup"; R.id.chip_cooldown -> "cooldown"; else -> "Exercise"
            }
            val chip = dialogBinding.targetMuscleChipGroup.findViewById<Chip>(dialogBinding.targetMuscleChipGroup.checkedChipId)
            adapter.setFilter(cat, if (chip?.text == "All") null else chip?.text?.toString())
        }
        dialogBinding.categoryChipGroup.setOnCheckedStateChangeListener { _, _ -> filter() }
        dialogBinding.targetMuscleChipGroup.setOnCheckedStateChangeListener { _, _ -> filter() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter(
            onItemClick = { item, _, _ ->
                if (viewModel.isWorkoutActive.value && viewModel.activeWorkoutId.value != item.assignment.workoutId) {
                    showToast("Another workout is in progress!"); return@ExerciseAdapter
                }
                val onlyEx = exerciseAdapter.currentList.filterIsInstance<WorkoutItem.ExerciseItem>()
                val idx = onlyEx.indexOfFirst { it.detail.assignment.exerciseId == item.assignment.exerciseId && it.detail.assignment.category == item.assignment.category }
                if (idx != -1) {
                    viewModel.workout.value?.let { navigationViewModel.setCurrentWorkoutData(it) }
                    navigationViewModel.navigateToExerciseDetail(
                        workoutId = item.assignment.workoutId, 
                        exerciseId = item.assignment.exerciseId, 
                        category = item.assignment.category,
                        position = idx + 1, 
                        total = onlyEx.size
                    )
                }
            },
            onListUpdated = { list -> 
                // BUG FIX: Ensure we merge with hidden exercises (Warmup/Cooldown) to prevent accidental deletion
                val currentWorkout = viewModel.workout.value ?: return@ExerciseAdapter
                val updatedAssignments = list.map { it.assignment }
                
                val allExisting = currentWorkout.exerciseAssignments.map { it.assignment }
                val hiddenAssignments = allExisting.filter { existing ->
                    updatedAssignments.none { it.exerciseId == existing.exerciseId && it.category == existing.category }
                }
                
                // RECALCULATE ORDERS to prevent "jumping" exercises
                // We strictly separate order space by category: Warmup (0-99), Exercise (100-199), Cooldown (200+)
                val combined = (updatedAssignments + hiddenAssignments).sortedWith(compareBy(
                    { when(it.category.lowercase()) { "warmup" -> 0; "exercise" -> 1; else -> 2 } },
                    { it.order }
                ))
                
                val finalAssignments = combined.mapIndexed { index, assignment ->
                    assignment.copy(order = index)
                }
                
                viewModel.updateWorkout(currentWorkout.workout, finalAssignments)
            },
            onDragStart = { vh -> itemTouchHelper.startDrag(vh) }
        )
        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseAdapter; itemAnimator = null; isNestedScrollingEnabled = false
        }
        itemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(exerciseAdapter))
        itemTouchHelper.attachToRecyclerView(binding.exercisesRecyclerView)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.workout.collect { workout ->
                    workout?.let {
                        binding.tvToolbarTitle.text = it.workout.name
                        binding.workoutTitle.text = it.workout.name
                        
                        val resId = ImageUtil.getWorkoutImageResource(it.workout.targetMuscle, it.workout.name, it.workout.difficulty)
                        Glide.with(this@WorkoutDetailFragment)
                            .load(resId)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(binding.workoutBannerImage)

                        isSettingInitialState = true
                        binding.switchIncludeWarmupCooldown.isChecked = it.workout.includeWarmupCooldown
                        isSettingInitialState = false
                        updateDisplayList(it, it.workout.includeWarmupCooldown)
                        
                        // Update progress bar based on filtered list consistency
                        val include = it.workout.includeWarmupCooldown
                        val filtered = if (include) {
                            it.exerciseAssignments
                        } else {
                            it.exerciseAssignments.filter { assignment -> assignment.assignment.category.equals("Exercise", true) }
                        }
                        val total = filtered.size.coerceAtLeast(1)
                        val done = filtered.count { assignment -> assignment.assignment.isCompleted }
                        binding.workoutProgress.progress = (done.toFloat() / total) * 100
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.isWorkoutActive, 
                    viewModel.activeWorkoutId, 
                    viewModel.workout, 
                    userViewModel.user.asFlow(), 
                    navigationViewModel.workoutNavRequest
                ) { isActive, activeId, currWorkout, user, navRequest -> 
                    val isThis = isActive && activeId == currWorkout?.workout?.id
                    val isLocked = navRequest.isFromPersonalized && navRequest.dayIndex != -1 && navRequest.dayIndex > (user?.lastCompletedWorkoutDay ?: 0)
                    Triple(isThis, isActive, isLocked)
                }.collect { (isThis, any, isLocked) ->
                    binding.startButton.apply {
                        visibility = if (isLocked || isThis || !any) View.VISIBLE else View.GONE
                        isEnabled = !isLocked
                        alpha = if (isLocked) 0.5f else 1.0f
                        text = if (isLocked) "LOCKED" else if (isThis) "RESUME" else "START"
                    }
                    binding.activeWorkoutBar.visibility = if (isThis) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.elapsedTimeSeconds.collect { binding.tvActiveTimer.text = viewModel.formatElapsedTime(it) }
            }
        }
    }

    private fun updateDisplayList(workout: WorkoutWithExercises, include: Boolean) {
        val list = mutableListOf<WorkoutItem>()
        val assignments = workout.exerciseAssignments.sortedBy { it.assignment.order }
        val warm = assignments.filter { it.assignment.category.equals("warmup", true) }
        val cool = assignments.filter { it.assignment.category.equals("cooldown", true) }
        val main = assignments.filter { it.assignment.category.equals("Exercise", true) }

        if (include) {
            binding.workoutExerciseCount.text = assignments.size.toString()
            if (warm.isNotEmpty()) { list.add(WorkoutItem.DividerItem("Warm-up")); list.addAll(warm.map { WorkoutItem.ExerciseItem(it) }) }
            list.add(WorkoutItem.DividerItem("Main Workout")); list.addAll(main.map { WorkoutItem.ExerciseItem(it) })
            if (cool.isNotEmpty()) { list.add(WorkoutItem.DividerItem("Cool-down")); list.addAll(cool.map { WorkoutItem.ExerciseItem(it) }) }
        } else {
            binding.workoutExerciseCount.text = main.size.toString()
            list.addAll(main.map { WorkoutItem.ExerciseItem(it) })
        }
        binding.workoutDuration.text = WorkoutUtil.calculateTotalDuration(assignments, include)
        exerciseAdapter.submitList(list)
    }
}

package com.example.nutriority.ui.workout

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nutriority.R
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.databinding.DialogAboutExerciseBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.gson.Gson

class AboutExerciseBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogAboutExerciseBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAboutExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exerciseJson = arguments?.getString(ARG_EXERCISE)
        val exercise = Gson().fromJson(exerciseJson, Exercise::class.java)

        exercise?.let { setupData(it) }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            
            // Calculate 90% of screen height
            val displayMetrics = requireContext().resources.displayMetrics
            val targetHeight = (displayMetrics.heightPixels * 0.90).toInt()
            
            // Force the bottom sheet height to 90%
            val layoutParams = it.layoutParams
            layoutParams.height = targetHeight
            it.layoutParams = layoutParams
            
            // Force full expansion to the 90% height and disable collapsing
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.peekHeight = targetHeight
        }
    }

    private fun setupData(exercise: Exercise) {
        binding.tvExerciseName.text = exercise.name.uppercase()
        
        // Display Instructions from Array
        if (exercise.instructions.isNotEmpty()) {
            binding.tvInstructions.text = exercise.instructions.joinToString("\n") { it.trim() }
        } else {
            binding.tvInstructions.text = exercise.description
        }
        
        // Setup Target and Secondary Muscle Chips
        binding.cgTargetMuscle.removeAllViews()
        val primaryDark = ContextCompat.getColor(requireContext(), R.color.primary_dark)
        
        // Combine target and secondary for chip display
        val muscleTags = mutableListOf<String>()
        if (exercise.target.isNotBlank()) muscleTags.addAll(exercise.target.split(",").map { it.trim() })
        if (exercise.secondary.isNotBlank()) muscleTags.addAll(exercise.secondary.split(",").map { it.trim() })
        
        muscleTags.filter { it.isNotBlank() }.distinct().forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                chipBackgroundColor = ColorStateList.valueOf(primaryDark)
                setTextColor(Color.WHITE)
                chipStrokeWidth = 0f
            }
            binding.cgTargetMuscle.addView(chip)
        }

        // Load GIF using Glide from URL
        if (exercise.gifUrl.isNotBlank()) {
            Glide.with(this)
                .asGif()
                .load(exercise.gifUrl)
                .placeholder(R.drawable.img_balanced_diet)
                .error(R.drawable.img_balanced_diet)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.ivExerciseVisual)
        } else if (exercise.imageResId != 0) {
            binding.ivExerciseVisual.setImageResource(exercise.imageResId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EXERCISE = "arg_exercise"

        fun newInstance(exercise: Exercise): AboutExerciseBottomSheet {
            val args = Bundle()
            args.putString(ARG_EXERCISE, Gson().toJson(exercise))
            val fragment = AboutExerciseBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }
}

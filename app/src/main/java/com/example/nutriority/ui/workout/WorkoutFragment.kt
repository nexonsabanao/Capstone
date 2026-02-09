package com.example.nutriority.ui.workout

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentWorkoutBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutFragment : BaseBindingFragment<FragmentWorkoutBinding>(FragmentWorkoutBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupTabs()
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) TrainerWorkoutsFragment() else CustomWorkoutsFragment()
            }
        }

        binding.workoutViewPager.apply {
            this.adapter = adapter
            offscreenPageLimit = 1 // Optimization: Keep tabs in memory for instant switching
            
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateTabUI(position == 0)
                }
            })
        }
    }

    private fun setupTabs() {
        binding.tabTrainer.setOnClickListener { binding.workoutViewPager.currentItem = 0 }
        binding.tabCustom.setOnClickListener { binding.workoutViewPager.currentItem = 1 }
    }

    private fun updateTabUI(isTrainer: Boolean) {
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_chip_selected_dark)
        val transparentBg = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val grayColor = Color.parseColor("#757575")

        if (isTrainer) {
            binding.tabTrainer.background = selectedBg
            binding.tabTrainer.setTextColor(whiteColor)
            binding.tabCustom.setBackgroundColor(transparentBg)
            binding.tabCustom.setTextColor(grayColor)
        } else {
            binding.tabCustom.background = selectedBg
            binding.tabCustom.setTextColor(whiteColor)
            binding.tabTrainer.setBackgroundColor(transparentBg)
            binding.tabTrainer.setTextColor(grayColor)
        }
    }
}

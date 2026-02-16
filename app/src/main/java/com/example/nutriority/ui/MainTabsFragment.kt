package com.example.nutriority.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentMainTabsBinding
import com.example.nutriority.ui.home.AllMealsFragment
import com.example.nutriority.ui.home.ArticleDetailFragment
import com.example.nutriority.ui.home.HomeFragment
import com.example.nutriority.ui.meal.MealDetailFragment
import com.example.nutriority.ui.meal.MealFragment
import com.example.nutriority.ui.profile.EditProfileFragment
import com.example.nutriority.ui.profile.LogManualFragment
import com.example.nutriority.ui.profile.ProfileFragment
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.workout.AllWorkoutsFragment
import com.example.nutriority.ui.workout.ExerciseDetailFragment
import com.example.nutriority.ui.workout.ExerciseLibraryFragment
import com.example.nutriority.ui.workout.PersonalizedWorkoutFragment
import com.example.nutriority.ui.workout.WorkoutCompleteFragment
import com.example.nutriority.ui.workout.WorkoutDetailFragment
import com.example.nutriority.ui.workout.WorkoutFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainTabsFragment : BaseBindingFragment<FragmentMainTabsBinding>(FragmentMainTabsBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupViewPager()
        setupBottomNavigation()
        observeNavigation()
        handleBackPress()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupViewPager() {
        binding.viewPager.apply {
            adapter = TabsAdapter(this@MainTabsFragment)
            offscreenPageLimit = 2
            isUserInputEnabled = false
            
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val isPrimaryTab = position < 4
                    binding.bottomNavigationView.isVisible = isPrimaryTab
                    binding.separator.isVisible = isPrimaryTab

                    if (isPrimaryTab) {
                        val itemId = when (position) {
                            0 -> R.id.navigation_home
                            1 -> R.id.navigation_workout
                            2 -> R.id.navigation_meal
                            3 -> R.id.navigation_profile
                            else -> R.id.navigation_home
                        }
                        if (binding.bottomNavigationView.selectedItemId != itemId) {
                            binding.bottomNavigationView.selectedItemId = itemId
                        }
                    }
                }
            })
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val page = when (item.itemId) {
                R.id.navigation_home -> 0
                R.id.navigation_workout -> 1
                R.id.navigation_meal -> 2
                R.id.navigation_profile -> 3
                else -> 0
            }
            navigationViewModel.setTab(page)
            true
        }
    }

    private fun observeNavigation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.currentTab.collect { tabIndex ->
                    binding.viewPager.post {
                        if (binding.viewPager.currentItem != tabIndex) {
                            binding.viewPager.setCurrentItem(tabIndex, false)
                        }
                    }
                }
            }
        }
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navigationViewModel.goBack()) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private inner class TabsAdapter(fragment: androidx.fragment.app.Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 15

        override fun createFragment(position: Int): androidx.fragment.app.Fragment = when (position) {
            0 -> HomeFragment()
            1 -> WorkoutFragment()
            2 -> MealFragment()
            3 -> ProfileFragment()
            4 -> PersonalizedWorkoutFragment()
            5 -> ExerciseLibraryFragment()
            6 -> AllWorkoutsFragment()
            7 -> MealDetailFragment()
            8 -> ArticleDetailFragment()
            9 -> WorkoutDetailFragment()
            10 -> ExerciseDetailFragment()
            11 -> WorkoutCompleteFragment()
            12 -> EditProfileFragment()
            13 -> LogManualFragment()
            14 -> AllMealsFragment()
            else -> HomeFragment()
        }
    }
}

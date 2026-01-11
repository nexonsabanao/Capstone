package com.example.nutriority.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentMainTabsBinding
import com.example.nutriority.ui.home.ArticleDetailFragment
import com.example.nutriority.ui.home.HomeFragment
import com.example.nutriority.ui.meal.MealDetailFragment
import com.example.nutriority.ui.meal.MealFragment
import com.example.nutriority.ui.profile.ProfileFragment
import com.example.nutriority.ui.workout.AllWorkoutsFragment
import com.example.nutriority.ui.workout.ExerciseLibraryFragment
import com.example.nutriority.ui.workout.PersonalizedWorkoutFragment
import com.example.nutriority.ui.workout.WorkoutDetailFragment
import com.example.nutriority.ui.workout.WorkoutFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainTabsFragment : Fragment() {

    private var _binding: FragmentMainTabsBinding? = null
    private val binding get() = _binding!!

    private val navigationViewModel: NavigationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }

        setupViewPager()
        setupBottomNavigation()
        observeNavigation()
        handleBackPress()
    }

    private fun setupViewPager() {
        val adapter = TabsAdapter(this)
        binding.viewPager.adapter = adapter
        
        // keeps ALL fragments alive in memory for instant switching
        binding.viewPager.offscreenPageLimit = 9 
        
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val page = when (item.itemId) {
                R.id.navigation_home -> 0
                R.id.navigation_workout -> 1
                R.id.navigation_meal -> 2
                R.id.navigation_profile -> 3
                else -> 0
            }
            if (binding.viewPager.currentItem != page) {
                navigationViewModel.setTab(page)
            }
            true
        }
    }

    private fun observeNavigation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.currentTab.collect { tabIndex ->
                    if (binding.viewPager.currentItem != tabIndex) {
                        binding.viewPager.setCurrentItem(tabIndex, false)
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

    private inner class TabsAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 10

        override fun createFragment(position: Int): Fragment {
            return when (position) {
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
                else -> HomeFragment()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

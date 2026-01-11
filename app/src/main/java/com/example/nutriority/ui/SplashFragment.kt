package com.example.nutriority.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentSplashBinding
import com.example.nutriority.ui.home.HomeViewModel
import com.example.nutriority.ui.meal.MealViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    // Scoping ViewModels to Activity so data persists into the fragments
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val mealViewModel: MealViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (onBoardingIsFinished()) {
            // Pre-load data while the splash screen is visible
            observeAndPreload()
        } else {
            // Onboarding not finished, just wait and go to onboarding
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(2000)
                findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
            }
        }
    }

    private fun observeAndPreload() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Combine multiple state flows/livedata to wait for all critical data across ALL 4 tabs
                combine(
                    homeViewModel.allWorkouts,
                    homeViewModel.allMeals,
                    homeViewModel.allArticles,
                    mealViewModel.mealPlan.asFlow(), // Tab 3: Meal Plan
                    userViewModel.user.asFlow()      // Tab 4: Profile/User Data
                ) { workouts, meals, articles, plan, user ->
                    // Data is "Ready" when library content and user profile are loaded
                    workouts.isNotEmpty() && meals.isNotEmpty() && articles.isNotEmpty() && user != null
                }.collectLatest { isDataReady ->
                    if (isDataReady) {
                        // All data is cached in ViewModels, safe to navigate
                        findNavController().navigate(R.id.action_splashFragment_to_mainTabsFragment)
                    }
                }
            }
        }
        
        // Safety timeout: navigate anyway after 5 seconds to avoid getting stuck
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(5000)
            if (findNavController().currentDestination?.id == R.id.splashFragment) {
                findNavController().navigate(R.id.action_splashFragment_to_mainTabsFragment)
            }
        }
    }

    private fun onBoardingIsFinished(): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("Finished", false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

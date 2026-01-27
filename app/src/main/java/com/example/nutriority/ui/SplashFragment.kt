package com.example.nutriority.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.example.nutriority.data.model.Article
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.databinding.FragmentSplashBinding
import com.example.nutriority.ui.home.HomeViewModel
import com.example.nutriority.ui.meal.MealViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var mealRepository: MealRepository
    
    private val homeViewModel: HomeViewModel by activityViewModels()
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

        // Force library load on app start using fragment scope to ensure persistence
        lifecycleScope.launch {
            try {
                // 1. Wait for library initialization to FINISH
                workoutRepository.ensureLibraryIsLoaded()
                mealRepository.ensureLibraryIsLoaded()
                
                // 2. Determine Navigation Path
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    observeAndPreload(true)
                } else if (onBoardingIsFinished()) {
                    observeAndPreload(false)
                } else {
                    kotlinx.coroutines.delay(1500)
                    findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
                }
            } catch (e: Exception) {
                Log.e("Splash", "Library load failed", e)
                findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
            }
        }
    }

    private fun observeAndPreload(isAuth: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    homeViewModel.unfilteredWorkouts,
                    homeViewModel.allMeals,
                    homeViewModel.allArticles,
                    userViewModel.user.asFlow()
                ) { workouts: List<Workout>, meals: List<Meal>, articles: List<Article>, user: User? ->
                    workouts.isNotEmpty() && meals.isNotEmpty() && articles.isNotEmpty() && user != null
                }.collectLatest { isDataReady ->
                    if (isDataReady) {
                        val user = userViewModel.user.value
                        val hasPlan = !user?.personalizedPlanJson.isNullOrBlank()
                        
                        if (isAuth && hasPlan) {
                            findNavController().navigate(R.id.action_splashFragment_to_mainTabsFragment)
                        } else {
                            findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
                        }
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(6000)
            if (findNavController().currentDestination?.id == R.id.splashFragment) {
                findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
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

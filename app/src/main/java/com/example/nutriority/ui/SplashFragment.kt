package com.example.nutriority.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.RecommendedWorkoutRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.databinding.FragmentSplashBinding
import com.example.nutriority.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var mealRepository: MealRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var recommendedWorkoutRepository: RecommendedWorkoutRepository
    
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

        lifecycleScope.launch {
            try {
                // 1. Initialize core libraries
                workoutRepository.ensureLibraryIsLoaded()
                mealRepository.ensureLibraryIsLoaded()
                
                // 2. Seed Official Trainer Workouts
                val allExercises = workoutRepository.getAllExercises().firstOrNull() ?: emptyList()
                if (allExercises.isNotEmpty()) {
                    recommendedWorkoutRepository.seedOfficialWorkouts(allExercises)
                }
                
                // 3. Check Authentication
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    var localUser = userRepository.getInitialUser()
                    if (localUser == null) {
                        userRepository.restoreUserFromCloud()
                        localUser = userRepository.getInitialUser()
                    }

                    if (localUser != null) {
                        findNavController().navigate(R.id.action_splashFragment_to_mainTabsFragment)
                    } else {
                        findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
                    }
                } else {
                    delay(1500)
                    findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
                }
            } catch (e: Exception) {
                Log.e("Splash", "Navigation failed", e)
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

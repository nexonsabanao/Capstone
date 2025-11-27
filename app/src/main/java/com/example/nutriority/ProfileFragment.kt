package com.example.nutriority

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.nutriority.R
import com.example.nutriority.data.User // This is your provided User.kt
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentProfileBinding // Import the generated binding class
import com.google.android.material.chip.Chip

class ProfileFragment : Fragment() {

    // ViewBinding variables for safe access to UI elements
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ViewModel declaration
    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using the binding class
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Get an instance of your UserViewModel
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        // 2. Observe the 'user' LiveData from the ViewModel
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            // This block runs when data is loaded from the database
            // and whenever it changes.
            user?.let {
                // 3. Populate the UI with the retrieved user data
                bindUserDataToViews(it)
            }
        }
    }
    private fun bindUserDataToViews(user: User) {
        // Use 'binding.apply' for cleaner code
        binding.apply {
            // Physical Stats Section
            profileGender.text = user.gender
            // Use string resources for better formatting and localization
            profileHeight.text = getString(R.string.height_format, user.heightCm)
            profileWeight.text = getString(R.string.weight_format, user.weightKg)

            // Goals & Lifestyle Section
            profileGoal.text = user.goal // Updated from primaryGoal
            profileActivityLevel.text = user.activityLevel

            // Preferences Section
            profilePreferredDiet.text = user.preferredDiet
            profileWorkoutPreference.text = user.workoutPreference // Updated from workoutStyle

            // Excluded Ingredients (ChipGroup)
            profileExcludedIngredients.removeAllViews() // Clear old chips before adding new ones
            user.excludedIngredients.forEach { ingredient ->
                val chip = Chip(context).apply {
                    text = ingredient
                    // Optional: You can style the chip here
                }
                profileExcludedIngredients.addView(chip)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevent memory leaks by nullifying the binding object
        _binding = null
    }
}

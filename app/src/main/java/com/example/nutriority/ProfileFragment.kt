package com.example.nutriority

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import com.example.nutriority.R
import com.example.nutriority.data.User
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentProfileBinding
import com.google.android.material.chip.Chip

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                bindUserDataToViews(it)
            }
        }
    }

    private fun bindUserDataToViews(user: User) {
        binding.apply {
            profileGender.text = user.gender
            profileHeight.text = getString(R.string.height_format, user.heightCm)
            profileWeight.text = getString(R.string.weight_format, user.weightKg)
            profileGoal.text = user.goal
            profileActivityLevel.text = user.activityLevel
            profilePreferredDiet.text = user.preferredDiet
            profileAge.text = user.age?.takeIf { it > 0 }?.toString()
                ?: getString(R.string.no_age_available)

            profileExcludedIngredients.removeAllViews()
            user.excludedIngredients.forEach { ingredient ->
                val chip = Chip(context).apply {
                    text = ingredient
                }
                profileExcludedIngredients.addView(chip)
            }

            val summary = requireActivity()
                .getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
                .getString("personalized_plan_summary", null)

            profilePlanSummary.text = summary ?: getString(R.string.no_plan_available)
        }
    }

    // MOVED THIS FUNCTION BACK INSIDE THE CLASS
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} // <-- This is now the final closing brace for the class

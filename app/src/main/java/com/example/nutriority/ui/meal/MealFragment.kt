package com.example.nutriority.ui.meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentMealBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MealFragment : Fragment() {

    private var _binding: FragmentMealBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.genderTextview.text = it.gender
                binding.weightTextview.text = it.weightKg.toString()
                binding.heightTextview.text = it.heightCm.toString()
                binding.ageTextview.text = it.age.toString()
                binding.activityTextview.text = it.activityLevel
                binding.dietTextview.text = it.preferredDiet
                binding.excludedIngredientsTextview.text = it.excludedIngredients.joinToString(", ")
                binding.goalTextview.text = it.goal
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

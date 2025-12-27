package com.example.nutriority.ui.meal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository
) : ViewModel() {

    val allMeals: LiveData<List<Meal>> = mealRepository.allMeals.asLiveData()

}

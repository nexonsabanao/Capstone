package com.example.nutriority.ui.home

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentAllMealsBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.AllMealAdapter
import com.example.nutriority.ui.adapter.AllMealItem
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import com.example.nutriority.data.model.Meal
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllMealsFragment : BaseBindingFragment<FragmentAllMealsBinding>(FragmentAllMealsBinding::inflate) {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val mealAdapter by lazy {
        AllMealAdapter { meal ->
            navigationViewModel.navigateToMealDetail(Gson().toJson(meal))
        }
    }

    private val searchQuery = MutableStateFlow("")
    private val filterType = MutableStateFlow("All")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        binding.rvAllMeals.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = mealAdapter
            mealAdapter.setupSpanManager(this)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { 
            KeyboardUtil.hideKeyboard(requireActivity())
            navigationViewModel.goBack() 
        }
        
        binding.etSearch.doAfterTextChanged { 
            searchQuery.value = it.toString().lowercase().trim() 
        }

        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                KeyboardUtil.hideKeyboard(requireActivity())
                v.clearFocus()
                true
            } else false
        }

        binding.chipFilter.setOnClickListener { showFilterMenu() }
    }

    private fun showFilterMenu() {
        // Use ContextThemeWrapper to force a light/white background for the PopupMenu
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_Light_PopupMenu)
        val popup = PopupMenu(wrapper, binding.chipFilter)
        
        popup.menu.add("All")
        popup.menu.add("Breakfast")
        popup.menu.add("Lunch")
        popup.menu.add("Dinner")
        // "Snack" removed as per request
        
        popup.setOnMenuItemClickListener { item ->
            filterType.value = item.title.toString()
            binding.chipFilter.text = item.title
            true
        }
        popup.show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    homeViewModel.allMeals,
                    searchQuery,
                    filterType
                ) { meals, query, filter ->
                    val filtered = meals?.filter { meal ->
                        val matchesSearch = meal.name.lowercase().contains(query) || 
                                          meal.ingredients.any { it.lowercase().contains(query) }
                        val matchesFilter = filter == "All" || meal.mealTime.equals(filter, ignoreCase = true)
                        matchesSearch && matchesFilter
                    }
                    
                    val sectionedList = mutableListOf<AllMealItem>()
                    val groups = filtered?.groupBy { it.mealTime }
                    
                    val order = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                    order.forEach { time ->
                        groups?.get(time)?.let { mealList ->
                            sectionedList.add(AllMealItem.Header(time))
                            sectionedList.addAll(mealList.map { AllMealItem.MealItem(it) })
                        }
                    }
                    groups?.filterKeys { !order.contains(it) }?.forEach { (time, mealList) ->
                        sectionedList.add(AllMealItem.Header(time))
                        sectionedList.addAll(mealList.map { AllMealItem.MealItem(it) })
                    }
                    
                    sectionedList
                }.collect { filteredList ->
                    mealAdapter.submitList(filteredList)
                    binding.tvEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}

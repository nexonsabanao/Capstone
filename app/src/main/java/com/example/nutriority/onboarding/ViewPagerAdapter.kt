package com.example.nutriority.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.nutriority.onboarding.screens.*

class ViewPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fm, lifecycle) {

    private val totalPages = 9

    override fun getItemCount(): Int {
        return totalPages
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment()
            1 -> FirstScreen()
            2 -> SecondScreen()
            3 -> ThirdScreen()
            4 -> WorkoutPreference()
            5 -> FourthScreen()
            6 -> FifthScreen()
            7 -> SixthScreen()
            8 -> SeventhScreen()
            else -> throw IllegalStateException("Requested a fragment for an invalid position: $position")
        }
    }
}

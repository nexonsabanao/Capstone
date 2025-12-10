package com.example.nutriority.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.nutriority.ui.onboarding.screens.AgeFragment
import com.example.nutriority.ui.onboarding.screens.FifthScreen
import com.example.nutriority.ui.onboarding.screens.FirstScreen
import com.example.nutriority.ui.onboarding.screens.FourthScreen
import com.example.nutriority.ui.onboarding.screens.SecondScreen
import com.example.nutriority.ui.onboarding.screens.SeventhScreen
import com.example.nutriority.ui.onboarding.screens.SixthScreen
import com.example.nutriority.ui.onboarding.screens.ThirdScreen
import com.example.nutriority.ui.onboarding.screens.WelcomeFragment

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
            3 -> AgeFragment()
            4 -> ThirdScreen()
            5 -> FourthScreen()
            6 -> FifthScreen()
            7 -> SixthScreen()
            8 -> SeventhScreen()
            else -> throw IllegalStateException("Requested a fragment for an invalid position: $position")
        }
    }
}

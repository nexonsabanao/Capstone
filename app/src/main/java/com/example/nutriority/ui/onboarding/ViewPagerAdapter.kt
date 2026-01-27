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
import com.example.nutriority.ui.onboarding.screens.LoginFragment

class ViewPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fm, lifecycle) {

    private val totalPages = 10 // Increased to 10 to accommodate Login

    override fun getItemCount(): Int {
        return totalPages
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LoginFragment() // NEW: Enforce student email first
            1 -> WelcomeFragment()
            2 -> FirstScreen()
            3 -> SecondScreen()
            4 -> AgeFragment()
            5 -> ThirdScreen()
            6 -> FourthScreen()
            7 -> FifthScreen()
            8 -> SixthScreen()
            9 -> SeventhScreen()
            else -> throw IllegalStateException("Requested a fragment for an invalid position: $position")
        }
    }
}

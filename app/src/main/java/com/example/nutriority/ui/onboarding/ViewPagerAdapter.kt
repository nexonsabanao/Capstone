package com.example.nutriority.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.nutriority.ui.onboarding.screens.*

class ViewPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fm, lifecycle) {

    private val totalPages = 12 // Increased to 12 to accommodate ForgotPassword

    override fun getItemCount(): Int {
        return totalPages
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LoginFragment()
            1 -> SignUpFragment()
            2 -> ForgotPasswordFragment() // Added ForgotPasswordFragment
            3 -> WelcomeFragment()
            4 -> FirstScreen()
            5 -> SecondScreen()
            6 -> AgeFragment()
            7 -> ThirdScreen()
            8 -> FourthScreen()
            9 -> FifthScreen()
            10 -> SixthScreen()
            11 -> SeventhScreen()
            else -> throw IllegalStateException("Requested a fragment for an invalid position: $position")
        }
    }
}

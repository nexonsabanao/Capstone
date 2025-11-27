package com.example.nutriority.onboarding

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.nutriority.databinding.FragmentViewPagerBinding
import com.example.nutriority.onboarding.screens.*

class ViewPagerFragment : Fragment() {

    private var _binding: FragmentViewPagerBinding? = null
    private val binding get() = _binding!!
    private var lastNavigationTime = 0L
    private val navigationDebounceInterval = 400L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupNavigationListeners()
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(
            childFragmentManager,
            lifecycle
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }

    private fun setupNavigationListeners() {
        childFragmentManager.setFragmentResultListener("navigationRequestNext", this) { _, _ ->
            navigateToNextScreen()
        }

        childFragmentManager.setFragmentResultListener("navigationRequestPrevious", this) { _, _ ->
            navigateToPreviousScreen()
        }
    }

    private fun canNavigate(): Boolean {
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastNavigationTime > navigationDebounceInterval) {
            lastNavigationTime = currentTime
            return true
        }
        return false
    }

    private fun navigateToNextScreen() {
        if (!canNavigate() || binding.viewPager.scrollState != ViewPager2.SCROLL_STATE_IDLE) {
            return
        }

        val nextItem = binding.viewPager.currentItem + 1
        if (nextItem < (binding.viewPager.adapter?.itemCount ?: 0)) {
            binding.viewPager.setCurrentItem(nextItem, true)
        }
    }

    private fun navigateToPreviousScreen() {
        if (!canNavigate() || binding.viewPager.scrollState != ViewPager2.SCROLL_STATE_IDLE) {
            return
        }

        val prevItem = binding.viewPager.currentItem - 1
        if (prevItem >= 0) {
            binding.viewPager.setCurrentItem(prevItem, true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

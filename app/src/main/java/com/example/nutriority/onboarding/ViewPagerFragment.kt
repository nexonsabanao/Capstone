package com.example.nutriority.onboarding

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nutriority.BaseFragment
import androidx.viewpager2.widget.ViewPager2
import com.example.nutriority.databinding.FragmentViewPagerBinding
import com.example.nutriority.onboarding.screens.*

class ViewPagerFragment : BaseFragment() {

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

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

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

    override fun onStart() {
        super.onStart()

        // Notify child fragments which page is selected. We use a fragment-result so that
        // children can decide whether to start expensive work only when they become visible.
        // Register the callback once and keep a reference so it can be removed onDestroyView.
        if (pageChangeCallback == null) {
            pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    childFragmentManager.setFragmentResult("pageSelected", Bundle().apply {
                        putInt("position", position)
                    })
                }
            }
            binding.viewPager.registerOnPageChangeCallback(pageChangeCallback!!)
        }
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
        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        _binding = null
    }
}

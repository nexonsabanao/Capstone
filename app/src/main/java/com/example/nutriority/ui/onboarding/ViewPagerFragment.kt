package com.example.nutriority.ui.onboarding

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.nutriority.databinding.FragmentViewPagerBinding
import kotlin.math.abs

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

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupNavigationListeners()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPager.currentItem > 0) {
                    navigateToPreviousScreen()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(
            childFragmentManager,
            lifecycle
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        // This will create a fading animation between fragments
        binding.viewPager.setPageTransformer(FadePageTransformer())
        binding.viewPager.offscreenPageLimit = 9
    }

    private class FadePageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            view.alpha = 1 - abs(position)
        }
    }

    override fun onStart() {
        super.onStart()

        if (pageChangeCallback == null) {
            pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    childFragmentManager.setFragmentResult("pageSelected", Bundle().apply {
                        putInt("position", position)
                    })
                }

                override fun onPageScrollStateChanged(state: Int) {
                    val layerType = if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        View.LAYER_TYPE_NONE
                    } else {
                        View.LAYER_TYPE_HARDWARE
                    }

                    if (binding.viewPager.childCount > 0) {
                        binding.viewPager.getChildAt(0).setLayerType(layerType, null)
                    }
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

        // Jump directly to Login (Position 0)
        childFragmentManager.setFragmentResultListener("navigationRequestLogin", this) { _, _ ->
            if (canNavigate()) {
                binding.viewPager.setCurrentItem(0, false)
            }
        }

        // Jump directly to ForgotPassword (Position 2)
        childFragmentManager.setFragmentResultListener("navigationRequestForgotPassword", this) { _, _ ->
            if (canNavigate()) {
                binding.viewPager.setCurrentItem(2, false)
            }
        }

        // Jump directly to Welcome (Position 3), skipping SignUp and ForgotPassword
        childFragmentManager.setFragmentResultListener("navigationRequestWelcome", this) { _, _ ->
            if (canNavigate()) {
                binding.viewPager.setCurrentItem(3, false)
            }
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
        binding.viewPager.adapter = null
        pageChangeCallback = null
        _binding = null
    }
}

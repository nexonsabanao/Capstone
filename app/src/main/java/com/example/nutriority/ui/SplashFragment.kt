package com.example.nutriority.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentSplashBinding
import com.example.nutriority.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            if (onBoardingIsFinished()) {
                // Wait until the data is ready for the first time.
                homeViewModel.isDataReady.first { it }

                // Data is ready, now navigate.
                delay(1000)
                val intent = Intent(requireContext(), BottomNavigationActivity::class.java)
                startActivity(intent)
                requireActivity().finish()

            } else {
                // Onboarding is not finished, go to onboarding after a delay.
                delay(3000) // Keep the original delay for the first-time user experience.
                findNavController().navigate(
                    R.id.action_splashFragment_to_viewPagerFragment,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.splashFragment, true)
                        .build()
                )
            }
        }
    }

    private fun onBoardingIsFinished(): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("Finished", false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

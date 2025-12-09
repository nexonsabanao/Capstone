package com.example.nutriority

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.nutriority.databinding.FragmentSplashBinding
import com.example.nutriority.ui.BottomNavigationActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

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
            // Wait for 3 seconds (3000 milliseconds).
            delay(3000)

            if (onBoardingIsFinished()) {
                val intent = Intent(requireContext(), BottomNavigationActivity::class.java)
                startActivity(intent)

                requireActivity().finish()
            } else {
                findNavController().navigate(
                    R.id.action_splashFragment_to_viewPagerFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.splashFragment, true)
                        .build()
                )
            }
        }
    }

    // --- THIS IS THE NEW FUNCTION ---
    // Function to check the value in SharedPreferences.
    private fun onBoardingIsFinished(): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        // Read the "Finished" key. If it doesn't exist (first launch), default to 'false'.
        return sharedPref.getBoolean("Finished", false)
    }
    // --- END OF NEW FUNCTION ---

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding reference to prevent memory leaks.
        _binding = null
    }
}
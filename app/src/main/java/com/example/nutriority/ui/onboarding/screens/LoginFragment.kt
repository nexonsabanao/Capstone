package com.example.nutriority.ui.onboarding.screens

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var mealRepository: MealRepository
    
    private val userViewModel: UserViewModel by activityViewModels()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    @Suppress("DEPRECATION")
    private var googleSignInClient: GoogleSignInClient? = null

    @Suppress("DEPRECATION")
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // If status code is 10, it confirms the SHA-1/Support Email issue
                showError("Google Error (${e.statusCode}): Please check Firebase SHA-1 and Support Email.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoogleSignIn()

        binding.etEmail.doAfterTextChanged { binding.tvError.visibility = View.INVISIBLE }
        binding.etPassword.doAfterTextChanged { binding.tvError.visibility = View.INVISIBLE }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().lowercase().trim()
            val password = binding.etPassword.text.toString().trim()
            
            when {
                email.isBlank() -> showError("Please enter your CVSU email")
                password.isBlank() -> showError("Please enter your password")
                !isValidStudentEmail(email) -> showError("Invalid: Access restricted to CVSU students (tmc.xxxx@cvsu.edu.ph)")
                else -> performFirebaseLogin(email, password)
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            googleSignInClient?.let { client ->
                @Suppress("DEPRECATION")
                val signInIntent = client.signInIntent
                googleSignInLauncher.launch(signInIntent)
            } ?: showError("Initializing... please try again in a moment.")
        }
    }

    @Suppress("DEPRECATION")
    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        } catch (e: Exception) {
            Log.e("Login", "Google Client Init Failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        binding.btnGoogleSignIn.isEnabled = false
        binding.btnGoogleSignIn.text = "Authenticating..."

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val email = user?.email ?: ""
                    
                    if (isValidStudentEmail(email)) {
                        restoreAndProceed()
                    } else {
                        @Suppress("DEPRECATION")
                        googleSignInClient?.signOut()
                        auth.signOut()
                        showError("Access restricted: Please use your @cvsu.edu.ph student email.")
                        resetButtons()
                    }
                } else {
                    showError("Firebase Auth failed: ${task.exception?.message}")
                    resetButtons()
                }
            }
    }

    private fun performFirebaseLogin(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "AUTHENTICATING..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    restoreAndProceed()
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { createTask ->
                            if (createTask.isSuccessful) {
                                restoreAndProceed()
                            } else {
                                resetButtons()
                                showError("Authentication failed: ${createTask.exception?.message}")
                            }
                        }
                }
            }
    }

    private fun restoreAndProceed() {
        binding.btnLogin.isEnabled = false
        binding.btnGoogleSignIn.isEnabled = false
        binding.btnLogin.text = "RESTORING DATA..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                coroutineScope {
                    // Start all restoration tasks in parallel
                    val profileJob = async { userRepository.restoreUserFromCloud() }
                    val historyJob = async { workoutRepository.restoreHistoryFromCloud() }
                    val mealsJob = async { mealRepository.restoreMealsFromCloud() }
                    
                    profileJob.await()
                    historyJob.await()
                    mealsJob.await()
                }
                
                // Extra safety: Wait for Room to finalize writes before moving
                delay(1000)
                
                parentFragmentManager.setFragmentResult("navigationRequestNext", Bundle())
            } catch (e: Exception) {
                Log.e("Login", "Restore process failed", e)
                parentFragmentManager.setFragmentResult("navigationRequestNext", Bundle())
            }
        }
    }

    private fun resetButtons() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "LOGIN"
        binding.btnGoogleSignIn.isEnabled = true
        binding.btnGoogleSignIn.text = "Sign in with Google"
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun isValidStudentEmail(email: String): Boolean {
        val lowerEmail = email.lowercase().trim()
        return lowerEmail.startsWith("tmc.") && lowerEmail.endsWith("@cvsu.edu.ph")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

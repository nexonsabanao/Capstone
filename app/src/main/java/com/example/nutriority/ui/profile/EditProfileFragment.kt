package com.example.nutriority.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.MainActivity
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.model.User
import com.example.nutriority.databinding.FragmentEditProfileBinding
import com.example.nutriority.ui.NavigationViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFieldStaticContent()
        setupClickListeners()
        observeUserData()
    }

    private fun setupFieldStaticContent() {
        binding.rowAge.tvLabel.text = "Age"
        binding.rowAge.ivIcon.setImageResource(R.drawable.ic_calendar)

        binding.rowWeight.tvLabel.text = "Weight"
        binding.rowWeight.ivIcon.setImageResource(R.drawable.ic_scale_24)

        binding.rowHeight.tvLabel.text = "Height"
        binding.rowHeight.ivIcon.setImageResource(R.drawable.ic_height_24)

        binding.rowActivity.tvLabel.text = "Activity Level"
        binding.rowActivity.ivIcon.setImageResource(R.drawable.ic_exercise_24)

        binding.rowGoal.tvLabel.text = "Goal"
        binding.rowGoal.ivIcon.setImageResource(R.drawable.ic_fitness_24)

        binding.rowDiet.tvLabel.text = "Preferred Diet"
        binding.rowDiet.ivIcon.setImageResource(R.drawable.ic_award_meal_24)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            navigationViewModel.goBack()
        }

        binding.btnEditProfileImage.setOnClickListener {
            Toast.makeText(requireContext(), "Image picker coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.rowName.setOnClickListener { 
            showEditBottomSheet("Full Name", "What should we call you?", currentUser?.name ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS) { newVal ->
                updateUserField { it.copy(name = newVal) }
            }
        }

        binding.rowAge.root.setOnClickListener { 
            showEditBottomSheet("Age", "Enter your current age", currentUser?.age?.toString() ?: "", InputType.TYPE_CLASS_NUMBER) { newVal ->
                updateUserField { it.copy(age = newVal.toIntOrNull()) }
            }
        }

        binding.rowWeight.root.setOnClickListener { 
            showEditBottomSheet("Weight", "Enter your weight in kg", currentUser?.weightKg?.toString() ?: "", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL) { newVal ->
                updateUserField { it.copy(weightKg = newVal.toDoubleOrNull() ?: 0.0) }
            }
        }

        binding.rowHeight.root.setOnClickListener { 
            showEditBottomSheet("Height", "Enter your height in cm", currentUser?.heightCm?.toString() ?: "", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL) { newVal ->
                updateUserField { it.copy(heightCm = newVal.toDoubleOrNull() ?: 0.0) }
            }
        }

        binding.rowActivity.root.setOnClickListener { 
            val options = arrayOf("Sedentary", "Lightly active", "Active")
            showOptionsBottomSheet("Activity Level", "Choose your daily activity level", options) { selection ->
                updateUserField { it.copy(activityLevel = selection) }
            }
        }

        binding.rowGoal.root.setOnClickListener { 
            val options = arrayOf("Lose weight", "Keep fit", "Build muscle")
            showOptionsBottomSheet("Main Goal", "What do you want to achieve?", options) { selection ->
                updateUserField { it.copy(goal = selection) }
            }
        }

        binding.rowDiet.root.setOnClickListener { 
            val options = arrayOf("Balanced", "Low Carb", "Vegetarian")
            showOptionsBottomSheet("Preferred Diet", "Choose a nutrition style", options) { selection ->
                updateUserField { it.copy(preferredDiet = selection) }
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.clearAllLocalData()
            FirebaseAuth.getInstance().signOut()
            restartApp()
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("This will permanently erase ALL your progress. This action cannot be undone.")
            .setPositiveButton("DELETE EVERYTHING") { _, _ -> performFullDataWipe() }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun performFullDataWipe() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.collection("users").document(uid).delete()
                profileViewModel.clearAllLocalData()
                user.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Account Wiped Successfully", Toast.LENGTH_SHORT).show()
                        restartApp()
                    } else {
                        Toast.makeText(requireContext(), "Error: Re-login required to delete account.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun observeUserData() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            currentUser = user
            user?.let {
                binding.tvNameValue.text = if (it.name.isBlank()) "User" else it.name
                binding.rowAge.tvValue.text = it.age?.toString() ?: "0"
                binding.rowWeight.tvValue.text = "${it.weightKg} kg"
                binding.rowHeight.tvValue.text = "${it.heightCm} cm"
                binding.rowActivity.tvValue.text = it.activityLevel
                binding.rowGoal.tvValue.text = it.goal
                binding.rowDiet.tvValue.text = it.preferredDiet
                binding.tvGenderValue.text = it.gender
            }
        }
    }

    private fun updateUserField(action: (User) -> User) {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.updateOnboardingDataSuspend(action)
            userViewModel.restartWorkoutPlan()
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditBottomSheet(title: String, subtitle: String, currentVal: String, inputType: Int, onSave: (String) -> Unit) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_edit_field_bottom_sheet, null)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvSheetTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSheetSubtitle)
        val etValue = view.findViewById<TextInputEditText>(R.id.etFieldValue)
        val til = view.findViewById<TextInputLayout>(R.id.textInputLayout)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        tvTitle.text = "Edit $title"
        tvSubtitle.text = subtitle
        etValue.inputType = inputType
        etValue.setText(currentVal)
        etValue.setSelection(etValue.text?.length ?: 0)
        
        btnSave.setOnClickListener {
            val newVal = etValue.text.toString()
            if (newVal.isNotBlank()) {
                onSave(newVal)
                dialog.dismiss()
            } else {
                til.error = "Field cannot be empty"
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showOptionsBottomSheet(title: String, subtitle: String, options: Array<String>, onSelect: (String) -> Unit) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_edit_field_bottom_sheet, null)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvSheetTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSheetSubtitle)
        val til = view.findViewById<TextInputLayout>(R.id.textInputLayout)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        
        til.visibility = View.GONE
        btnSave.visibility = View.GONE
        
        tvTitle.text = title
        tvSubtitle.text = subtitle
        
        val container = view.findViewById<ViewGroup>(R.id.optionsContainer) ?: (view as ViewGroup)
        options.forEach { option ->
            val itemView = layoutInflater.inflate(R.layout.item_selection_option, container, false)
            val tvOption = itemView.findViewById<TextView>(R.id.tvOptionText)
            tvOption.text = option
            itemView.setOnClickListener {
                onSelect(option)
                dialog.dismiss()
            }
            container.addView(itemView)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

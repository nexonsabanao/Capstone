package com.example.nutriority.ui.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.nutriority.MainActivity
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.model.User
import com.example.nutriority.databinding.FragmentEditProfileBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.util.AgeUtil
import com.example.nutriority.ui.util.DatePickerUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private var currentUser: User? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            processAndSaveProfileImage(uri)
        }
    }

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
        binding.rowAge.tvLabel.text = "Birthday"
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
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.rowName.setOnClickListener { 
            showEditBottomSheet("Full Name", "What should we call you?", currentUser?.name ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS) { newVal ->
                if (newVal != currentUser?.name) {
                    updateUserField(false) { it.copy(name = newVal) }
                }
            }
        }

        binding.rowAge.root.setOnClickListener { 
            DatePickerUtil.showDatePicker(requireContext(), currentUser?.birthDate) { selection ->
                if (selection != currentUser?.birthDate) {
                    updateUserField(false) { it.copy(birthDate = selection) }
                }
            }
        }

        binding.rowWeight.root.setOnClickListener { 
            showEditBottomSheet("Weight", "Enter your weight in kg", currentUser?.weightKg?.toString() ?: "", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL) { newVal ->
                val newWeight = newVal.toDoubleOrNull() ?: 0.0
                if (newWeight != currentUser?.weightKg) {
                    updateUserField(false) { it.copy(weightKg = newWeight) }
                }
            }
        }

        binding.rowHeight.root.setOnClickListener { 
            showEditBottomSheet("Height", "Enter your height in cm", currentUser?.heightCm?.toString() ?: "", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL) { newVal ->
                val newHeight = newVal.toDoubleOrNull() ?: 0.0
                if (newHeight != currentUser?.heightCm) {
                    updateUserField(false) { it.copy(heightCm = newHeight) }
                }
            }
        }

        binding.rowActivity.root.setOnClickListener { 
            val options = arrayOf("Sedentary", "Lightly active", "Active")
            showOptionsBottomSheet("Activity Level", "Choose your daily activity level", options) { selection ->
                if (selection != currentUser?.activityLevel) {
                    showUpdateOptionsDialog("Activity Level") { shouldRestart ->
                        updateUserField(shouldRestart) { it.copy(activityLevel = selection) }
                    }
                }
            }
        }

        binding.rowGoal.root.setOnClickListener { 
            val options = arrayOf("Lose weight", "Keep fit", "Build muscle")
            showOptionsBottomSheet("Main Goal", "What do you want to achieve?", options) { selection ->
                if (selection != currentUser?.goal) {
                    showUpdateOptionsDialog("Goal") { shouldRestart ->
                        updateUserField(shouldRestart) { it.copy(goal = selection) }
                    }
                }
            }
        }

        binding.rowDiet.root.setOnClickListener { 
            val options = arrayOf("Balanced", "Low Carb", "Vegetarian")
            showOptionsBottomSheet("Preferred Diet", "Choose a nutrition style", options) { selection ->
                if (selection != currentUser?.preferredDiet) {
                    showUpdateOptionsDialog("Preferred Diet") { shouldRestart ->
                        updateUserField(shouldRestart) { it.copy(preferredDiet = selection) }
                    }
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun showUpdateOptionsDialog(fieldName: String, onSelection: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_plan_update_choice, null)
        
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnUpdate = dialogView.findViewById<MaterialButton>(R.id.btnUpdatePlan)
        val btnKeep = dialogView.findViewById<MaterialButton>(R.id.btnKeepCurrent)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        tvMessage.text = "You are changing your $fieldName. Would you like to update your current workout and nutrition plan to match this new setting, or keep your existing plan?"

        builder.setView(dialogView)
        val dialog = builder.create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnUpdate.setOnClickListener {
            onSelection(true)
            dialog.dismiss()
        }

        btnKeep.setOnClickListener {
            onSelection(false)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun processAndSaveProfileImage(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    null
                }
            }

            bitmap?.let {
                val path = convertToWebP(it)
                if (path != null) {
                    updateUserField(false) { user -> user.copy(profileImageUrl = path) }
                    Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun convertToWebP(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "profile_${System.currentTimeMillis()}.webp"
            val file = File(requireContext().filesDir, fileName)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
            out.flush()
            out.close()
            file.absolutePath
        } catch (e: Exception) {
            null
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
            .setNegativeButton("Cancel", null)
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
                
                // Display formatted birthdate
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                binding.rowAge.tvValue.text = it.birthDate?.let { date -> sdf.format(Date(date)) } ?: "Not set"
                
                binding.rowWeight.tvValue.text = "${it.weightKg} kg"
                binding.rowHeight.tvValue.text = "${it.heightCm} cm"
                binding.rowActivity.tvValue.text = it.activityLevel
                binding.rowGoal.tvValue.text = it.goal
                binding.rowDiet.tvValue.text = it.preferredDiet
                binding.tvGenderValue.text = it.gender

                if (it.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(File(it.profileImageUrl))
                        .placeholder(R.drawable.logo)
                        .circleCrop()
                        .into(binding.profileImage)
                }
            }
        }
    }

    private fun updateUserField(restartPlan: Boolean, action: (User) -> User) {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.updateOnboardingDataSuspend(action)
            if (restartPlan) {
                userViewModel.restartWorkoutPlan()
                Toast.makeText(requireContext(), "Profile updated and plan refreshed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            }
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

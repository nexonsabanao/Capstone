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
import androidx.core.text.HtmlCompat
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
import com.example.nutriority.ui.util.DatePickerUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
        binding.rowAge.tvLabel.text = "Birthday"
        binding.rowAge.ivIcon.setImageResource(R.drawable.ic_calendar)

        binding.rowGender.tvLabel.text = "Sex"
        binding.rowGender.ivIcon.setImageResource(R.drawable.outline_account_circle_50)

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

        binding.rowExclusions.tvLabel.text = "Excluded Ingredients"
        binding.rowExclusions.ivIcon.setImageResource(R.drawable.ic_allergy)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { navigationViewModel.goBack() }

        binding.rowName.setOnClickListener {
            showEditBottomSheet("Full Name", "What should we call you?", currentUser?.name ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS) { newVal ->
                if (newVal != currentUser?.name) {
                    updateUserField(false) { it.copy(name = newVal) }
                }
            }
        }

        binding.rowAge.root.setOnClickListener {
            DatePickerUtil.showDatePicker(requireContext(), currentUser?.birthDate) { selection ->
                val age = calculateAgeFromMillis(selection)
                if (age in 17..65) {
                    if (selection != currentUser?.birthDate) {
                        handleFieldUpdateWithPlanChoice("Birthday") { it.copy(birthDate = selection) }
                    }
                } else {
                    Toast.makeText(requireContext(), "Age must be between 17 and 65 years old", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.rowGender.root.setOnClickListener {
            val options = arrayOf("Male", "Female")
            showOptionsBottomSheet("Sex", "Select your biological sex", options) { selection ->
                if (selection != currentUser?.gender) {
                    handleFieldUpdateWithPlanChoice("Sex") { it.copy(gender = selection) }
                }
            }
        }

        binding.rowWeight.root.setOnClickListener {
            showWeightLogBottomSheet()
        }

        binding.rowHeight.root.setOnClickListener {
            showHeightLogBottomSheet()
        }

        binding.rowActivity.root.setOnClickListener {
            val options = arrayOf("Sedentary", "Lightly Active", "Active")
            showOptionsBottomSheet("Activity Level", "Choose your daily activity level", options) { selection ->
                if (selection != currentUser?.activityLevel) {
                    handleFieldUpdateWithPlanChoice("Activity Level") { it.copy(activityLevel = selection) }
                }
            }
        }

        binding.rowGoal.root.setOnClickListener {
            val options = arrayOf("Lose Weight", "Keep Fit", "Build Muscle")
            showOptionsBottomSheet("Main Goal", "What do you want to achieve?", options) { selection ->
                if (selection != currentUser?.goal) {
                    handleFieldUpdateWithPlanChoice("Goal") { it.copy(goal = selection) }
                }
            }
        }

        binding.rowDiet.root.setOnClickListener {
            val options = arrayOf("Balanced", "Low-Carb", "Vegetarian")
            showOptionsBottomSheet("Preferred Diet", "Choose a nutrition style", options) { selection ->
                if (selection != currentUser?.preferredDiet) {
                    handleFieldUpdateWithPlanChoice("Preferred Diet") { it.copy(preferredDiet = selection) }
                }
            }
        }

        binding.rowExclusions.root.setOnClickListener {
            val currentExclusions = currentUser?.excludedIngredients?.joinToString(", ") ?: ""
            showEditBottomSheet("Exclusions", "Enter ingredients to exclude (comma separated)", currentExclusions, InputType.TYPE_CLASS_TEXT) { newVal ->
                val newList = newVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (newList != currentUser?.excludedIngredients) {
                    handleFieldUpdateWithPlanChoice("Exclusions") { it.copy(excludedIngredients = newList) }
                }
            }
        }

        binding.btnAbout.setOnClickListener { showAboutDialog() }
        binding.btnTerms.setOnClickListener { showTermsDialog() }
        binding.btnLogout.setOnClickListener { showLogoutConfirmation() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteAccountConfirmation() }
    }

    private fun showAboutDialog() {
        val infoText = """
            <b>Nutriority</b> is a cutting-edge, personalized fitness and nutrition companion owned and operated by <b>Xfactor Fitness Gym</b>. Our mission is to transform lives by providing expert-level guidance that is accessible to everyone.<br><br>
            <b>Key Features:</b><br>
            • <b>Personalized Workout Plans:</b> Tailored to your fitness level, goals, and available equipment.<br>
            • <b>Nutrition Tracking:</b> Smart meal logging and diet plans based on your preferences.<br>
            • <b>Progress Monitoring:</b> Track your body metrics and workout consistency over time.<br>
            • <b>Expert Guidance:</b> Science-backed routines designed to maximize results and minimize injury risk.<br><br>
            Whether you are looking to lose weight, build muscle, or maintain a healthy lifestyle, <b>Nutriority</b> provides the tools and motivation you need to succeed.<br><br>
            
            <b>Research & Development:</b><br>
            <b>Study Conducted at:</b><br>
            XFactor Fitness Gym Trece<br><br>
            
            <b>Professional Guidance:</b><br>
            • Headcoach: Skylove Panaligan<br>
            • Nutritionist: Mark Anthony Rimando<br><br>
            
            <b>Creators & Researchers:</b><br>
            • Nexon Jr. Y. Sabañao<br>
            • Kenneth Ian B. Benedicto<br>
            • Ivan A. Pamaran<br><br>
            
            <i>Version 1.0.0</i>
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About Nutriority")
            .setMessage(HtmlCompat.fromHtml(infoText, HtmlCompat.FROM_HTML_MODE_COMPACT))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTermsDialog() {
        val termsAndServicesText = """
            Welcome to <b>Nutriority</b>! By using this app, you agree to the following terms. Please read them carefully.<br><br>
            <b>1. Who Can Use This App</b><br>
            Nutriority is a service provided by <b>Xfactor Fitness Gym</b>. By registering, you agree to provide accurate information to receive the best possible fitness and nutrition guidance.<br><br>
            <b>2. Health Disclaimer</b><br>
            ⚠ <b>IMPORTANT:</b> Nutriority is <b>NOT recommended</b> for users with significant health problems or injuries without medical clearance.<br><br>
            The workout plans and nutrition content in this app are for general wellness purposes only and do not constitute medical advice. Do not use this app if you have any of the following:<br>
            • Pre-existing medical conditions (e.g., heart disease, diabetes, hypertension)<br>
            • Current or recent injuries (e.g., joint, muscle, or spinal injuries)<br>
            • Any condition for which a doctor has advised you to avoid physical activity<br>
            • Pregnancy or postpartum recovery<br><br>
            If you are unsure whether this app is safe for you, please consult a licensed healthcare professional before use. <b>Stop using the app immediately</b> and seek medical attention if you feel pain, dizziness, or discomfort.<br><br>
            <b>3. User Responsibilities</b><br>
            By using Nutriority, you agree to:<br>
            • Provide honest and accurate information about yourself<br>
            • Use the app only for personal health and fitness purposes<br>
            • Not share your account with others<br>
            • Not misuse or attempt to damage the app or its data<br><br>
            <b>4. Privacy</b><br>
            Any personal information you provide (such as your name, age, and health data) will only be used to operate and improve Nutriority. Your data will be handled securely by <b>Xfactor Fitness Gym</b>.<br><br>
            <b>5. Limitation of Liability</b><br>
            Nutriority and <b>Xfactor Fitness Gym</b> are not liable for any injury, health issue, or damages that may result from using the app. You use the app at your own risk.<br><br>
            <b>6. Changes to These Terms</b><br>
            We may update these Terms from time to time. Continued use of the app after any changes means you accept the updated Terms.<br><br>
            <b>7. Contact</b><br>
            For questions or concerns, please reach out to the <b>Xfactor Fitness Gym</b> team through our official contact channels.<br><br>
            By using Nutriority, you confirm that you have read and agree to these Terms and Conditions.
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Terms and Conditions")
            .setMessage(HtmlCompat.fromHtml(termsAndServicesText, HtmlCompat.FROM_HTML_MODE_COMPACT))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showWeightLogBottomSheet() {
        val initialWeight = currentUser?.weightKg ?: 60.0
        val bottomSheet = WeightLogBottomSheetFragment(initialWeight, showDatePicker = false) { weight, date ->
            if (weight != currentUser?.weightKg) {
                handleFieldUpdateWithPlanChoice("Weight") { it.copy(weightKg = weight) }
                profileViewModel.logWeight(weight, date)
            }
        }
        bottomSheet.show(childFragmentManager, "WeightLogBottomSheet")
    }

    private fun showHeightLogBottomSheet() {
        val initialHeight = currentUser?.heightCm ?: 170.0
        val bottomSheet = HeightLogBottomSheetFragment(initialHeight) { height ->
            if (height != currentUser?.heightCm) {
                handleFieldUpdateWithPlanChoice("Height") { it.copy(heightCm = height) }
            }
        }
        bottomSheet.show(childFragmentManager, "HeightLogBottomSheet")
    }

    private fun isPlanReady(user: User?): Boolean {
        if (user == null) return false
        return user.birthDate != null &&
                user.heightCm > 0 &&
                user.weightKg > 0 &&
                user.gender.isNotBlank() &&
                user.activityLevel.isNotBlank() &&
                user.goal.isNotBlank() &&
                user.preferredDiet.isNotBlank()
    }

    private fun handleFieldUpdateWithPlanChoice(fieldName: String, updateAction: (User) -> User) {
        if (isPlanReady(currentUser)) {
            showUpdateOptionsDialog(fieldName) { shouldRestart ->
                updateUserField(shouldRestart, updateAction)
            }
        } else {
            updateUserField(false, updateAction)
        }
    }

    private fun calculateAgeFromMillis(millis: Long): Int {
        val dob = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--
        return age
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

        btnUpdate.setOnClickListener { onSelection(true); dialog.dismiss() }
        btnKeep.setOnClickListener { onSelection(false); dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
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
            .setMessage("This will permanently erase your account and ALL your progress. Are you sure?")
            .setPositiveButton("DELETE PERMANENTLY") { _, _ -> showSilentReauthDialog() }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showSilentReauthDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val dialogView = layoutInflater.inflate(R.layout.layout_edit_field_bottom_sheet, null)
        val etValue = dialogView.findViewById<TextInputEditText>(R.id.etFieldValue)
        val til = dialogView.findViewById<TextInputLayout>(R.id.textInputLayout)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)

        dialogView.findViewById<TextView>(R.id.tvSheetTitle).text = "Verify Password"
        dialogView.findViewById<TextView>(R.id.tvSheetSubtitle).text = "Please enter your password to confirm account deletion."
        etValue.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        etValue.hint = "Password"
        btnSave.text = "CONFIRM & DELETE"

        btnSave.setOnClickListener {
            val password = etValue.text.toString()
            if (password.isNotBlank()) { dialog.dismiss(); performReauthAndDeletion(password) }
            else til.error = "Password is required"
        }
        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun performReauthAndDeletion(password: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) performImmediateDeletion()
            else Toast.makeText(requireContext(), "Verification failed: ${reauthTask.exception?.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun performImmediateDeletion() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()
        user.delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        db.collection("users").document(uid).delete()
                        profileViewModel.clearAllLocalData()
                        Toast.makeText(requireContext(), "Account wiped successfully.", Toast.LENGTH_SHORT).show()
                        restartApp()
                    } catch (e: Exception) { restartApp() }
                }
            } else Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
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
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                binding.rowAge.tvValue.text = it.birthDate?.let { date -> sdf.format(Date(date)) } ?: "Not set"
                binding.rowGender.tvValue.text = it.gender
                binding.rowWeight.tvValue.text = "${it.weightKg} kg"

                // Fix: Round height to nearest whole number for display in the list row
                binding.rowHeight.tvValue.text = "${it.heightCm.roundToInt()} cm"

                binding.rowActivity.tvValue.text = it.activityLevel
                binding.rowGoal.tvValue.text = it.goal
                binding.rowDiet.tvValue.text = it.preferredDiet
                binding.rowExclusions.tvValue.text = if (it.excludedIngredients.isEmpty()) "None" else it.excludedIngredients.joinToString(", ")

                if (it.profileImageUrl.isNotEmpty()) {
                    Glide.with(this).load(it.profileImageUrl).placeholder(R.drawable.logo).circleCrop().into(binding.profileImage)
                }
            }
        }
    }

    private fun updateUserField(restartPlan: Boolean, action: (User) -> User) {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.updateOnboardingDataSuspend(action)
            if (restartPlan) {
                userViewModel.restartAllPlans()
                Toast.makeText(requireContext(), "Profile updated and plans refreshed", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditBottomSheet(title: String, subtitle: String, currentVal: String, inputType: Int, onSave: (String) -> Unit) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_edit_field_bottom_sheet, null)
        val etValue = view.findViewById<TextInputEditText>(R.id.etFieldValue)
        val til = view.findViewById<TextInputLayout>(R.id.textInputLayout)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        view.findViewById<TextView>(R.id.tvSheetTitle).text = "Edit $title"
        view.findViewById<TextView>(R.id.tvSheetSubtitle).text = subtitle
        etValue.inputType = inputType
        etValue.setText(currentVal)
        etValue.setSelection(etValue.text?.length ?: 0)

        btnSave.setOnClickListener {
            val newVal = etValue.text.toString()
            if (newVal.isNotBlank() || title == "Exclusions") { onSave(newVal); dialog.dismiss() }
            else til.error = "Field cannot be empty"
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showOptionsBottomSheet(title: String, subtitle: String, options: Array<String>, onSelect: (String) -> Unit) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_edit_field_bottom_sheet, null)
        val container = view.findViewById<ViewGroup>(R.id.optionsContainer) ?: (view as ViewGroup)

        view.findViewById<TextView>(R.id.tvSheetTitle).text = title
        view.findViewById<TextView>(R.id.tvSheetSubtitle).text = subtitle
        view.findViewById<View>(R.id.textInputLayout).visibility = View.GONE
        view.findViewById<View>(R.id.btnSave).visibility = View.GONE

        options.forEach { option ->
            val itemView = layoutInflater.inflate(R.layout.item_selection_option, container, false)
            val tvOption = itemView.findViewById<TextView>(R.id.tvOptionText)
            tvOption.text = option
            itemView.setOnClickListener { onSelect(option); dialog.dismiss() }
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

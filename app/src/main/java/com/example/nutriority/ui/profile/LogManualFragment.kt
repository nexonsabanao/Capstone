package com.example.nutriority.ui.profile

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Filter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.R
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.databinding.FragmentLogManualBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class IngredientInfo(
    val name: String,
    val kcal: Double, // per 100g
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val category: String
)

data class SelectedIngredient(
    val info: IngredientInfo,
    val grams: Double
)

@AndroidEntryPoint
class LogManualFragment : BaseBindingFragment<FragmentLogManualBinding>(FragmentLogManualBinding::inflate) {

    private val navigationViewModel: NavigationViewModel by activityViewModels()
    @Inject lateinit var mealRepository: MealRepository

    private val selectedIngredients = mutableListOf<SelectedIngredient>()

    private val commonIngredients = listOf(
        // RICE & GRAINS
        IngredientInfo("🍚 White Rice (Cooked)", 130.0, 2.7, 28.0, 0.3, "Rice & Grains"),
        IngredientInfo("🌾 Brown Rice (Cooked)", 111.0, 2.6, 23.0, 0.9, "Rice & Grains"),
        IngredientInfo("🧄 Sinangag (Garlic Fried Rice)", 163.0, 3.0, 31.0, 4.5, "Rice & Grains"),
        IngredientInfo("🍚 Malagkit (Sticky Rice, Cooked)", 97.0, 2.0, 21.0, 0.2, "Rice & Grains"),
        IngredientInfo("🥣 Lugaw (Rice Porridge)", 52.0, 1.2, 11.0, 0.2, "Rice & Grains"),
        IngredientInfo("🥣 Champorado (Chocolate Rice)", 110.0, 3.0, 22.0, 2.5, "Rice & Grains"),
        IngredientInfo("🍚 White Rice (Cooked)", 130.0, 2.7, 28.0, 0.3, "Rice & Grains"),
        IngredientInfo("🥣 Oatmeal", 68.0, 2.4, 12.0, 1.4, "Rice & Grains"),
        IngredientInfo("🍞 Bread (Whole Wheat)", 247.0, 13.0, 41.0, 3.4, "Rice & Grains"),

        // MEAT & POULTRY
        IngredientInfo("🍗 Chicken Breast", 165.0, 31.0, 0.0, 3.6, "Meat & Poultry"),
        IngredientInfo("🥚 Egg", 155.0, 13.0, 1.1, 11.0, "Meat & Poultry"),
        IngredientInfo("🥩 Beef (Lean)", 250.0, 26.0, 0.0, 15.0, "Meat & Poultry"),
        IngredientInfo("🥓 Pork Belly (Liempo)", 518.0, 9.0, 0.0, 53.0, "Meat & Poultry"),
        IngredientInfo("🌭 Longganisa (Pork/Sweet)", 335.0, 13.0, 2.0, 30.0, "Meat & Poultry"),
        IngredientInfo("🥓 Tocino (Cured Pork)", 320.0, 14.0, 25.0, 18.0, "Meat & Poultry"),
        IngredientInfo("🥩 Beef Tapa (Cured Beef)", 210.0, 24.0, 3.0, 11.0, "Meat & Poultry"),

        // SEAFOOD
        IngredientInfo("🐟 Salmon", 208.0, 20.0, 0.0, 13.0, "Seafood"),
        IngredientInfo("🐟 Tuna", 132.0, 28.0, 0.0, 1.3, "Seafood"),
        IngredientInfo("🐟 Bangus (Milkfish)", 148.0, 19.0, 0.0, 7.0, "Seafood"),
        IngredientInfo("🐟 Galunggong (Round Scad)", 101.0, 18.6, 0.0, 2.5, "Seafood"),
        IngredientInfo("🐟 Tilapia", 128.0, 20.0, 0.0, 2.7, "Seafood"),
        IngredientInfo("🐟 Tinapa (Smoked Fish)", 145.0, 24.0, 0.0, 4.5, "Seafood"),
        IngredientInfo("🐟 Dilis (Dried Anchovies)", 210.0, 45.0, 0.0, 3.2, "Seafood"),
        IngredientInfo("🦐 Shrimp (Hipon)", 99.0, 24.0, 0.0, 0.3, "Seafood"),
        IngredientInfo("🦪 Tahong (Mussels)", 86.0, 12.0, 3.7, 2.2, "Seafood"),

        // STREET FOOD & SNACKS
        IngredientInfo("🥚 Balut (Fertilized Egg)", 188.0, 14.0, 1.0, 14.0, "Street Food & Snacks"),
        IngredientInfo("🍢 Fish Balls (Fried)", 160.0, 8.0, 18.0, 6.0, "Street Food & Snacks"),
        IngredientInfo("🍢 Kikiam (Street Style)", 190.0, 9.0, 15.0, 10.0, "Street Food & Snacks"),
        IngredientInfo("🥚 Kwek-Kwek (Quail Egg)", 210.0, 10.0, 12.0, 14.0, "Street Food & Snacks"),
        IngredientInfo("🍢 Isaw (Grilled Intestines)", 150.0, 14.0, 2.0, 10.0, "Street Food & Snacks"),
        IngredientInfo("🍢 Pork BBQ Skewer", 230.0, 18.0, 8.0, 14.0, "Street Food & Snacks"),
        IngredientInfo("🥓 Chicharon (Pork Cracklings)", 544.0, 61.0, 0.0, 31.0, "Street Food & Snacks"),
        IngredientInfo("🍞 SkyFlakes Crackers", 460.0, 9.0, 68.0, 17.0, "Street Food & Snacks"),
        IngredientInfo("🍌 Banana Cue (Fried Saba)", 220.0, 1.5, 45.0, 5.0, "Street Food & Snacks"),
        IngredientInfo("🍌 Turon (Banana Spring Roll)", 260.0, 2.0, 48.0, 7.0, "Street Food & Snacks"),

        // VEGETABLES & LEGUMES
        IngredientInfo("🥦 Broccoli", 34.0, 2.8, 7.0, 0.4, "Vegetables & Legumes"),
        IngredientInfo("🥔 Potato", 77.0, 2.0, 17.0, 0.1, "Vegetables & Legumes"),
        IngredientInfo("🍃 Spinach", 23.0, 2.9, 3.6, 0.4, "Vegetables & Legumes"),
        IngredientInfo("🥕 Carrots", 41.0, 0.9, 10.0, 0.2, "Vegetables & Legumes"),
        IngredientInfo("🧊 Tokwa (Tofu)", 76.0, 8.0, 1.9, 4.8, "Vegetables & Legumes"),
        IngredientInfo("🥬 Kangkong (Water Spinach)", 19.0, 2.6, 3.1, 0.2, "Vegetables & Legumes"),
        IngredientInfo("🍃 Malunggay (Moringa Leaves)", 64.0, 9.4, 8.2, 1.4, "Vegetables & Legumes"),
        IngredientInfo("🍐 Sayote (Chayote)", 19.0, 0.8, 4.5, 0.1, "Vegetables & Legumes"),
        IngredientInfo("🥒 Sitaw (Yardlong Beans)", 47.0, 2.8, 8.0, 0.4, "Vegetables & Legumes"),
        IngredientInfo("🎃 Kalabasa (Squash)", 26.0, 1.0, 6.5, 0.1, "Vegetables & Legumes"),
        IngredientInfo("🥒 Ampalaya (Bitter Melon)", 17.0, 1.0, 3.7, 0.2, "Vegetables & Legumes"),
        IngredientInfo("🍆 Talong (Eggplant)", 25.0, 1.0, 6.0, 0.2, "Vegetables & Legumes"),
        IngredientInfo("🥬 Pechay (Native)", 13.0, 1.5, 2.2, 0.2, "Vegetables & Legumes"),
        IngredientInfo("🥕 Labanos (Radish)", 18.0, 0.6, 4.1, 0.1, "Vegetables & Legumes"),
        IngredientInfo("🥒 Sigarilyas (Winged Bean)", 49.0, 7.0, 4.0, 1.0, "Vegetables & Legumes"),
        IngredientInfo("🥒 Upong (Bottle Gourd)", 14.0, 0.6, 3.4, 0.1, "Vegetables & Legumes"),
        IngredientInfo("🫘 Munggo (Mung Beans, Dried)", 341.0, 24.0, 63.0, 1.2, "Vegetables & Legumes"),

        // FRUITS & TUBERS
        IngredientInfo("🍌 Banana", 89.0, 1.1, 23.0, 0.3, "Fruits & Tubers"),
        IngredientInfo("🥑 Avocado", 160.0, 2.0, 8.5, 15.0, "Fruits & Tubers"),
        IngredientInfo("🥭 Mango (Carabao)", 60.0, 0.8, 15.0, 0.4, "Fruits & Tubers"),
        IngredientInfo("🥭 Papaya (Ripe)", 43.0, 0.5, 11.0, 0.3, "Fruits & Tubers"),
        IngredientInfo("🍌 Banana (Saba, Raw)", 122.0, 1.3, 31.0, 0.1, "Fruits & Tubers"),
        IngredientInfo("🍌 Banana (Latundan)", 90.0, 1.0, 23.0, 0.3, "Fruits & Tubers"),
        IngredientInfo("🍞 Pandesal", 310.0, 10.0, 58.0, 4.5, "Fruits & Tubers"),
        IngredientInfo("🍠 Ube (Purple Yam)", 120.0, 1.5, 27.0, 0.1, "Fruits & Tubers"),
        IngredientInfo("🍠 Kamote (Sweet Potato)", 86.0, 1.6, 20.0, 0.1, "Fruits & Tubers"),
        IngredientInfo("🍠 Gabi (Taro)", 112.0, 1.5, 26.0, 0.2, "Fruits & Tubers"),
        IngredientInfo("🍠 Cassava (Kamoteng Kahoy)", 160.0, 1.4, 38.0, 0.3, "Fruits & Tubers"),
        IngredientInfo("🥥 Buko (Coconut Water)", 19.0, 0.7, 3.7, 0.2, "Fruits & Tubers"),
        IngredientInfo("🥥 Buko Meat (Young)", 354.0, 3.3, 15.0, 33.0, "Fruits & Tubers"),

        // CONDIMENTS & DAIRY
        IngredientInfo("🥛 Milk (Whole)", 61.0, 3.2, 4.8, 3.3, "Condiments & Dairy"),
        IngredientInfo("🍶 Patis (Fish Sauce)", 35.0, 5.0, 3.7, 0.0, "Condiments & Dairy"),
        IngredientInfo("🍶 Toyo (Soy Sauce)", 53.0, 8.0, 4.9, 0.1, "Condiments & Dairy"),
        IngredientInfo("🍶 Suka (Cane Vinegar)", 18.0, 0.0, 0.0, 0.0, "Condiments & Dairy"),
        IngredientInfo("🦐 Bagoong Alamang (Shrimp Paste)", 100.0, 12.0, 4.0, 4.0, "Condiments & Dairy"),
        IngredientInfo("🐟 Bagoong Monamon (Fish Paste)", 75.0, 10.0, 2.0, 1.5, "Condiments & Dairy"),
        IngredientInfo("🍅 Banana Ketchup", 115.0, 1.0, 27.0, 0.2, "Condiments & Dairy"),
        IngredientInfo("🍶 Mang Tomas (Sauce)", 135.0, 1.0, 32.0, 0.1, "Condiments & Dairy"),
        IngredientInfo("🥛 Coconut Milk (Gata)", 230.0, 2.3, 5.5, 24.0, "Condiments & Dairy"),
        IngredientInfo("🥤 Kalamansi Juice", 30.0, 0.5, 7.0, 0.1, "Condiments & Dairy"),
        IngredientInfo("🥛 Condensed Milk", 321.0, 7.9, 54.0, 8.7, "Condiments & Dairy"),
        IngredientInfo("🥛 Evaporated Milk", 135.0, 6.8, 10.0, 7.6, "Condiments & Dairy"),
        IngredientInfo("🍝 Filipino Spaghetti Sauce", 100.0, 2.0, 16.0, 2.5, "Condiments & Dairy")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clearFields()
        setupDropdown()
        setupClickListeners()
        updateIngredientsUi()
    }

    private fun clearFields() {
        selectedIngredients.clear()
        binding.etTitle.setText("")
        binding.etCalories.setText("")
        binding.etProtein.setText("")
        binding.etCarbs.setText("")
        binding.etFats.setText("")
        binding.spinnerMealTime.setText("Breakfast", false)
        updateIngredientsUi()
    }

    private fun setupDropdown() {
        val items = listOf("Breakfast", "Lunch", "Dinner", "Snack")
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, items)
        binding.spinnerMealTime.setAdapter(adapter)
        binding.spinnerMealTime.setOnClickListener {
            binding.spinnerMealTime.showDropDown()
        }
        binding.spinnerMealTime.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            binding.spinnerMealTime.setText(selectedItem, false)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { 
            KeyboardUtil.hideKeyboard(requireActivity())
            navigationViewModel.goBack() 
        }
        binding.btnAddIngredient.setOnClickListener { showAddIngredientDialog() }
        binding.btnLogMeal.setOnClickListener { 
            KeyboardUtil.hideKeyboard(requireActivity())
            saveMeal() 
        }
    }

    private fun showAddIngredientDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_ingredient, null)
        val spinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerIngredient)
        val etWeight = dialogView.findViewById<TextInputEditText>(R.id.etWeight)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
        val btnAdd = dialogView.findViewById<View>(R.id.btnAdd)

        // Group ingredients and prepare items with headers
        val dropdownItems = mutableListOf<Any>()
        commonIngredients.groupBy { it.category }.forEach { (category, ingredients) ->
            dropdownItems.add(category) // Header
            dropdownItems.addAll(ingredients) // Items
        }

        val adapter = IngredientDropdownAdapter(requireContext(), dropdownItems)
        spinner.setAdapter(adapter)
        spinner.threshold = 1
        spinner.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) spinner.showDropDown() }
        spinner.setOnClickListener { spinner.showDropDown() }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            KeyboardUtil.hideKeyboard(requireActivity())
            dialog.dismiss()
        }

        btnAdd.setOnClickListener {
            val selectedName = spinner.text.toString().trim()
            val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
            
            val ingredientInfo = commonIngredients.find { it.name.equals(selectedName, ignoreCase = true) }
            
            if (ingredientInfo != null && weight > 0) {
                selectedIngredients.add(SelectedIngredient(ingredientInfo, weight))
                updateIngredientsUi()
                calculateTotals()
                dialog.dismiss()
            } else if (ingredientInfo == null) {
                Toast.makeText(requireContext(), "Ingredient not found. Please select from the list.", Toast.LENGTH_SHORT).show()
            } else if (weight <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid weight", Toast.LENGTH_SHORT).show()
            }
            KeyboardUtil.hideKeyboard(requireActivity())
        }

        dialog.show()
    }

    private fun calculateTotals() {
        var totalKcal = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFats = 0.0

        selectedIngredients.forEach { item ->
            val ratio = item.grams / 100.0
            totalKcal += item.info.kcal * ratio
            totalProtein += item.info.protein * ratio
            totalCarbs += item.info.carbs * ratio
            totalFats += item.info.fats * ratio
        }

        binding.etCalories.setText(totalKcal.roundToInt().toString())
        binding.etProtein.setText(totalProtein.roundToInt().toString())
        binding.etCarbs.setText(totalCarbs.roundToInt().toString())
        binding.etFats.setText(totalFats.roundToInt().toString())
    }

    private fun updateIngredientsUi() {
        binding.ingredientsContainer.removeAllViews()
        binding.tvIngredientEmpty.isVisible = selectedIngredients.isEmpty()

        selectedIngredients.forEachIndexed { index, item ->
            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 8, 0, 8)
                gravity = Gravity.CENTER_VERTICAL
            }

            val textView = TextView(requireContext()).apply {
                text = "• ${item.info.name} (${item.grams.roundToInt()}g)"
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val deleteIcon = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_close)
                layoutParams = LinearLayout.LayoutParams(48, 48)
                setPadding(8, 8, 8, 8)
                imageTintList = ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
                setOnClickListener {
                    selectedIngredients.removeAt(index)
                    updateIngredientsUi()
                    calculateTotals()
                }
            }

            itemLayout.addView(textView)
            itemLayout.addView(deleteIcon)
            binding.ingredientsContainer.addView(itemLayout)
        }
    }

    private fun saveMeal() {
        val title = binding.etTitle.text.toString().trim()
        val protein = binding.etProtein.text.toString().toIntOrNull() ?: 0
        val carbs = binding.etCarbs.text.toString().toIntOrNull() ?: 0
        val fats = binding.etFats.text.toString().toIntOrNull() ?: 0
        val calories = binding.etCalories.text.toString().toIntOrNull() ?: 0
        val mealTime = binding.spinnerMealTime.text.toString()

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Please add a title", Toast.LENGTH_SHORT).show()
            return
        }

        val ingredientsAsStrings = selectedIngredients.map { "${it.info.name} (${it.grams.roundToInt()}g)" }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                mealRepository.logManualMeal(title, protein, carbs, fats, mealTime, ingredientsAsStrings, calories)
                Toast.makeText(requireContext(), "Meal logged successfully!", Toast.LENGTH_SHORT).show()
                navigationViewModel.goBack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to log meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Custom adapter to handle headers and items in the AutoCompleteTextView
    private class IngredientDropdownAdapter(context: Context, private val originalItems: List<Any>) : 
        ArrayAdapter<Any>(context, 0, originalItems) {

        private var filteredItems: List<Any> = originalItems

        override fun getCount(): Int = filteredItems.size
        override fun getItem(position: Int): Any = filteredItems[position]

        override fun getViewTypeCount(): Int = 2
        override fun getItemViewType(position: Int): Int = if (getItem(position) is String) 0 else 1

        override fun isEnabled(position: Int): Boolean = getItem(position) is IngredientInfo

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            return if (item is String) {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_dropdown_header, parent, false)
                view.findViewById<TextView>(R.id.tvHeader).text = item
                view
            } else {
                val ingredient = item as IngredientInfo
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
                (view as TextView).text = ingredient.name
                view
            }
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.lowercase(Locale.ROOT) ?: ""
                    val results = FilterResults()
                    
                    if (query.isEmpty()) {
                        results.values = originalItems
                        results.count = originalItems.size
                    } else {
                        val filteredList = mutableListOf<Any>()
                        // Use original items to filter correctly while keeping headers
                        originalItems.filterIsInstance<String>().forEach { category ->
                            val itemsInCategory = originalItems.filterIsInstance<IngredientInfo>().filter { it.category == category }
                            val matchingIngredients = itemsInCategory.filter { 
                                it.name.lowercase(Locale.ROOT).contains(query) 
                            }
                            if (matchingIngredients.isNotEmpty()) {
                                filteredList.add(category)
                                filteredList.addAll(matchingIngredients)
                            }
                        }
                        results.values = filteredList
                        results.count = filteredList.size
                    }
                    return results
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    filteredItems = results?.values as? List<Any> ?: originalItems
                    notifyDataSetChanged()
                }

                override fun convertResultToString(resultValue: Any?): CharSequence {
                    return if (resultValue is IngredientInfo) resultValue.name else ""
                }
            }
        }
    }
}

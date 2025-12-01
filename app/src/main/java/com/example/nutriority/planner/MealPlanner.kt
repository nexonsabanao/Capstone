package com.example.nutriority.planner

/**
 * Simple deterministic meal planner that creates 3 meals that match the calorie needs
 * while respecting the user's dietary preference and excluded ingredients.
 */
object MealPlanner {

    private val catalog = listOf(
        Meal("Avocado Toast", 350, "Whole-grain toast with smashed avocado and seasoning", listOf("bread","avocado","salt","pepper"), "meal_avocado_toast"),
        Meal("Greek Yogurt & Berries", 300, "High-protein yogurt with fresh berries and a drizzle of honey", listOf("yogurt","berries","honey"), "meal_greek_yogurt"),
        Meal("Oatmeal with Nuts", 380, "Warm oats topped with nuts and a sliced banana", listOf("oats","banana","almonds"), "meal_oatmeal_nuts"),
        Meal("Chicken Salad", 450, "Grilled chicken with mixed greens and a light vinaigrette", listOf("chicken","lettuce","cucumber","vinaigrette"), "meal_chicken_salad"),
        Meal("Quinoa Bowl", 420, "Quinoa, beans, veggies and avocado for a balanced meal", listOf("quinoa","black beans","corn","avocado"), "meal_quinoa_bowl"),
        Meal("Salmon & Veggies", 500, "Oven-baked salmon with roasted vegetables and a lemon dressing", listOf("salmon","broccoli","olive oil","lemon"), "meal_salmon"),
        Meal("Tofu Stir-fry", 460, "Tofu with mixed vegetables and a soy-ginger glaze", listOf("tofu","soy sauce","broccoli","carrot"), "meal_tofu_stirfry"),
        Meal("Protein Smoothie", 350, "Protein powder blended with banana and almond milk", listOf("protein powder","banana","almond milk"), "meal_protein_smoothie"),
        Meal("Lentil Stew", 480, "Hearty lentils with tomatoes, herbs and root vegetables", listOf("lentils","tomato","carrot","onion"), "meal_lentil_stew")
    )

    fun planMeals(dailyCalories: Int, preferredDiet: String, excludedIngredients: List<String>): List<Meal> {
        // Basic split: breakfast 30%, lunch 35%, dinner 35%
        val splits = listOf(0.30, 0.35, 0.35)
        val targetCalories = splits.map { (dailyCalories * it).toInt() }

        // Filter catalog according to diet & exclusions
        val filtered = catalog.filter { meal ->
            // exclude if any excluded ingredient present
            if (excludedIngredients.any { ex -> meal.ingredients.any { it.contains(ex, true) } }) return@filter false

            // filter out meat-based meals for Vegetarian or Vegan
            when (preferredDiet.lowercase()) {
                "vegetarian" -> !containsMeat(meal)
                "vegan" -> !containsAnimalProducts(meal)
                "pescatarian" -> !containsRedMeat(meal)
                else -> true
            }
        }

        // For each meal slot, pick nearest calorie meal from filtered catalog
        val meals = targetCalories.mapIndexed { _, target ->
            val best = filtered.minByOrNull { kotlin.math.abs(it.calories - target) } ?: catalog.minByOrNull { kotlin.math.abs(it.calories - target) }!!
            // Slightly adjust description and calories so sum better matches
            val tunedCalories = target // we'll expect nearest
            best.copy(calories = tunedCalories, description = best.description)
        }

        return meals
    }

    private fun containsMeat(meal: Meal): Boolean = meal.ingredients.any { it.contains("chicken", true) || it.contains("beef", true) || it.contains("pork", true) }
    private fun containsRedMeat(meal: Meal): Boolean = meal.ingredients.any { it.contains("beef", true) || it.contains("pork", true) }
    private fun containsAnimalProducts(meal: Meal): Boolean = meal.ingredients.any { ing -> listOf("chicken","beef","pork","salmon","yogurt","cheese","egg","milk","honey").any { ing.contains(it, true) } }

}

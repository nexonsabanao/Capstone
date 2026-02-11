package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String = "",
    var description: String = "",
    var category: String = "",
    var targetMuscle: String = "",
    var imageName: String = "img_balanced_diet",
    var difficulty: String = "",
    var duration: String = "",
    var metValue: Double = 5.0,
    var tags: List<String> = emptyList(),
    var includeWarmupCooldown: Boolean = true
) {
    @get:Exclude
    @Ignore
    var imageResId: Int = 0
    
    @get:Exclude
    @Ignore
    var warmup: List<Warmup> = emptyList()
    
    @get:Exclude
    @Ignore
    var cooldown: List<Cooldown> = emptyList()
}

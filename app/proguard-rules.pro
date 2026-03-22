# Global Attributes
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# 1. Project Specific Models & Logic
# Essential for Room, Gson, Firebase, and your Planner logic
-keep class com.example.nutriority.data.model.** { *; }
-keep class com.example.nutriority.data.repository.** { *; }
-keep class com.example.nutriority.planner.** { *; }
-keep class com.example.nutriority.ui.util.** { *; }

# 2. Gson Rules
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.annotations.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 3. Room Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep @androidx.room.Entity class * { *; }
-keep interface * extends androidx.room.Dao { *; }
-dontwarn androidx.room.**

# 4. Hilt / Dagger Rules
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.appcompat.app.AppCompatActivity
-keep class com.example.nutriority.di.** { *; }
-keep class **_HiltModules* { *; }
-keep class **_HiltComponents* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.components.SingletonComponent class *

# 5. Glide Rules
-dontwarn com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep @com.bumptech.glide.annotation.GlideModule class * { *; }
-dontwarn com.bumptech.glide.**

# 6. Firebase & Firestore
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
# Crucial for Firestore to find the empty constructor for your models
-keepclassmembers class com.example.nutriority.data.model.** {
    public <init>(...);
}

# 7. MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# 8. Konfetti (Missing in your original)
-keep class nl.dionsegijn.konfetti.** { *; }

# 9. UI Libraries
-keep class com.mikhaellopez.circularprogressbar.** { *; }
-keep class me.relex.circleindicator.** { *; }

# 10. RootBeer (Security)
-keep class com.scottyab.rootbeer.** { *; }
-dontwarn com.scottyab.rootbeer.**

# 11. ViewBinding & Navigation
-keep class com.example.nutriority.databinding.** { *; }
-keep class androidx.navigation.** { *; }

# 12. Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

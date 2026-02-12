# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 1. Keep all data models and repositories (Essential for Room, Gson, Firebase, and Hilt)
-keep class com.example.nutriority.data.model.** { *; }
-keep class com.example.nutriority.data.repository.** { *; }
-keep class com.example.nutriority.planner.** { *; }
-keep class com.example.nutriority.ui.util.** { *; }

# 2. Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.annotations.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 3. Room specific rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep @androidx.room.Entity class * { *; }
-keep interface * extends androidx.room.Dao { *; }
-dontwarn androidx.room.**

# 4. Hilt / Dagger rules (Crucial for DI in Release)
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

# 5. RootBeer (Security)
-keep class com.scottyab.rootbeer.** { *; }
-dontwarn com.scottyab.rootbeer.**

# 6. MPAndroidChart rules
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# 7. UI Libraries (CircularProgressBar, CircleIndicator)
-keep class com.mikhaellopez.circularprogressbar.** { *; }
-keep class me.relex.circleindicator.** { *; }

# 8. Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# 9. Firebase & Firestore
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepclassmembers class com.example.nutriority.data.model.** {
    public <init>(...);
}

# 10. ViewBinding & Navigation
-keep class com.example.nutriority.databinding.** { *; }
-keep class androidx.navigation.** { *; }

# 11. Retain line numbers for better crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

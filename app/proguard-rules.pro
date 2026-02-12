# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 1. Keep all data models (Essential for Room & Gson)
-keep class com.example.nutriority.data.model.** { *; }
-keep class com.example.nutriority.planner.** { *; }

# 2. Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.annotations.** { *; }

# 3. Room specific rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep @androidx.room.Entity class * { *; }
-keep interface * extends androidx.room.Dao { *; }

# 4. Hilt / Dagger rules (Ensures DI doesn't fail in release)
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.appcompat.app.AppCompatActivity
-keep class com.example.nutriority.di.** { *; }

# 5. RootBeer (Security)
-keep class com.scottyab.rootbeer.** { *; }
-dontwarn com.scottyab.rootbeer.**

# 6. MPAndroidChart rules
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# 7. UI Libraries (CircularProgressBar, CircleIndicator)
-keep class com.mikhaellopez.circularprogressbar.** { *; }
-keep class me.relex.circleindicator.** { *; }

# 8. Kotlin Coroutines (Fixes internal dispatcher crashes)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$HandlerPost {
    private <init>(android.os.Handler, java.lang.String);
}

# 9. Firebase & Firestore
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.**

# 10. Retain line numbers for better crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

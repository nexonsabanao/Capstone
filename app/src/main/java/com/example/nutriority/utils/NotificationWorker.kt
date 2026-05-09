package com.example.nutriority.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.nutriority.MainActivity
import com.example.nutriority.NutriorityApp
import com.example.nutriority.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("nutriority_prefs", Context.MODE_PRIVATE)
        val lastUsageDate = prefs.getString("last_usage_date", "")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        Log.d("NotificationWorker", "Checking notification. Today: $today, Last used: $lastUsageDate")

        // If the app was already used today, don't show notification
        // Note: During testing, if you open the app, this will be true.
        if (lastUsageDate == today) {
            Log.d("NotificationWorker", "App already used today. Skipping notification.")
            // return Result.success() // Uncomment this in production to skip if used
        }

        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, NutriorityApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_green)
            .setContentTitle("Nutriority Reminder")
            .setContentText("Don't forget to track your meals and workouts today!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                
                with(NotificationManagerCompat.from(applicationContext)) {
                    // Use a unique ID based on current time
                    notify(System.currentTimeMillis().toInt(), builder.build())
                }
                Log.d("NotificationWorker", "Notification sent successfully.")
            } else {
                Log.w("NotificationWorker", "Notification permission not granted.")
            }
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error showing notification: ${e.message}")
        }
    }
}

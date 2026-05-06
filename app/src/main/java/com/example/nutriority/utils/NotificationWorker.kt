package com.example.nutriority.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
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

        // If the app was already used today, don't show notification
        if (lastUsageDate == today) {
            return Result.success()
        }

        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val builder = NotificationCompat.Builder(applicationContext, NutriorityApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Nutriority Reminder")
            .setContentText("You haven't checked your nutrition today. Stay consistent with your goals!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                with(NotificationManagerCompat.from(applicationContext)) {
                    notify(System.currentTimeMillis().toInt(), builder.build())
                }
            }
        } catch (e: SecurityException) {
            // Permission might have been revoked
        }
    }
}

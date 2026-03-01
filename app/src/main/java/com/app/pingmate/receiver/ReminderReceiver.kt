package com.app.pingmate.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.app.pingmate.MainActivity
import com.app.pingmate.R

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val REQUEST_CODE_GENERAL_BASE = 500000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isGeneral = intent.hasExtra("EXTRA_GENERAL_REMINDER_ID")
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "PingMate Reminder"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "You have a scheduled reminder"
        val notificationId = if (isGeneral) {
            intent.getIntExtra("EXTRA_GENERAL_REMINDER_ID", System.currentTimeMillis().toInt())
        } else {
            intent.getIntExtra("EXTRA_ID", System.currentTimeMillis().toInt())
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "pingmate_reminders",
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "pingmate_reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // Replacing with system reminder icon for now
            .setContentTitle("Alert: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(0xFF6B9DFE.toInt()) // NotiBlue
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}

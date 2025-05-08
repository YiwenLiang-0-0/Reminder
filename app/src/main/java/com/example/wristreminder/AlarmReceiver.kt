package com.example.wristreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.wristreminder.presentation.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context, intent)
    }

    private fun showNotification(context: Context, intent: Intent) {
        val channelId = "alarm_channel"

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        alarmIntent.putExtra("title", intent.getStringExtra("title"))
        alarmIntent.putExtra("sound", intent.getBooleanExtra("sound", true))
        alarmIntent.putExtra("time", intent.getStringExtra("time"))

        val pendingIntent = PendingIntent.getActivity(
            context, Int.MAX_VALUE, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a unique channel ID for each sound to ensure settings are applied
        val soundUri = intent.getStringExtra("soundUri") ?: ""
        val uniqueChannelId = if (soundUri.isNotEmpty()) {
            "alarm_channel_${soundUri.hashCode()}"
        } else {
            channelId
        }
        
        Log.d("AlarmReceiver", "Using sound URI: $soundUri")

        val channel = NotificationChannel(
            uniqueChannelId,
            "WristReminder",
            NotificationManager.IMPORTANCE_HIGH
        )
        
        // Get the custom sound URI if available
        val soundEnabled = intent.getBooleanExtra("sound", true)
        
        if (soundEnabled) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
                
            if (soundUri.isNotEmpty()) {
                try {
                    val uri = Uri.parse(soundUri)
                    channel.setSound(uri, audioAttributes)
                    Log.d("AlarmReceiver", "Setting custom sound: $soundUri")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error setting custom sound: ${e.message}")
                    // Fallback to default sound
                    channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
                }
            } else {
                // Use default notification sound
                channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
                Log.d("AlarmReceiver", "Using default sound")
            }
            channel.enableVibration(true)
        } else {
            channel.setSound(null, null)
            channel.enableVibration(false)
            Log.d("AlarmReceiver", "Sound disabled")
        }
        
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, uniqueChannelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("It's time!")
            .setContentText(intent.getStringExtra("title"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1, notification)
    }
}
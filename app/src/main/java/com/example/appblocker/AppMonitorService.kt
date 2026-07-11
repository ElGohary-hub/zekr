package com.example.appblocker

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import java.util.Locale

class AppMonitorService : AccessibilityService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val channelId = "AppBlockerChannel"

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val openedPackageName = event.packageName?.toString() ?: return

            val sharedPref = getSharedPreferences("AppConfigs", Context.MODE_PRIVATE)
            val savedConfig = sharedPref.getString(openedPackageName, null)

            if (savedConfig != null) {
                val parts = savedConfig.split("|")
                if (parts.size >= 3) {
                    val message = parts[0]
                    val useVoice = parts[1].toBoolean()
                    val useNotification = parts[2].toBoolean()

                    if (useVoice) speakText(message)
                    if (useNotification) sendNotification(openedPackageName, message)
                }
            }
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun sendNotification(appName: String, message: String) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("تنبيه لتطبيق")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "تنبيهات التطبيقات", NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ar")
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}


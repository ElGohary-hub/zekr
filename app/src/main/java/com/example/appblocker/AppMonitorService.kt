package com.example.appblocker

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

class AppMonitorService : AccessibilityService() {

    private val channelId = "AppBlockerChannel"
    private var mediaPlayer: MediaPlayer? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        createNotificationChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val openedPackageName = event.packageName?.toString() ?: return

            val sharedPref = getSharedPreferences("AppConfigs", Context.MODE_PRIVATE)
            val savedConfig = sharedPref.getString(openedPackageName, null)

            if (savedConfig != null) {
                val parts = savedConfig.split("|")
                if (parts.size >= 4) {
                    val message = parts[0]
                    val useNotification = parts[1].toBoolean()
                    val useVoice = parts[2].toBoolean()
                    val audioUriString = parts[3]

                    // تشغيل الإشعار
                    if (useNotification) {
                        sendNotification(openedPackageName, message)
                    }

                    // تشغيل الصوت اللي اختاره المستخدم
                    if (useVoice && audioUriString.isNotEmpty()) {
                        playUserAudio(audioUriString)
                    }
                }
            }
        }
    }

    private fun playUserAudio(uriString: String) {
        try {
            // إيقاف أي صوت قديم شغال
            mediaPlayer?.release()
            
            // تحويل النص إلى مسار حقيقي وقراءته
            val audioUri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer.create(this, audioUri)
            
            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            
            mediaPlayer?.start()
        } catch (e: Exception) {
            // تجاهل الخطأ في حالة تم مسح الملف من الهاتف
        }
    }

    private fun sendNotification(appName: String, message: String) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("تنبيه من AppBlocker")
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

    override fun onInterrupt() {}

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}

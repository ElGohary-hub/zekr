package com.example.appblocker

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

class AppMonitorService : AccessibilityService() {

    private val channelId = "AppBlockerChannel"
    private var mediaPlayer: MediaPlayer? = null
    
    private val lastTriggerTimeMap = mutableMapOf<String, Long>()
    
    // متغيرات للتحكم في التكرار التلقائي وإيقاف الضغطات الداخلية
    private var activePackage: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var reminderRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        createNotificationChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val openedPackageName = event.packageName?.toString() ?: return

            // 1. لو التطبيق المفتوح هو هو نفس التطبيق الحالي، متعملش حاجة (ده اللي بيمنع الإزعاج مع الضغطات الداخلية)
            if (openedPackageName == activePackage) {
                return
            }

            // 2. لو فتح تطبيق جديد أو طلع للشاشة الرئيسية
            activePackage = openedPackageName
            stopReminder() // نوقف المؤقت بتاع التطبيق القديم

            val sharedPref = getSharedPreferences("AppConfigs", Context.MODE_PRIVATE)
            val savedConfig = sharedPref.getString(openedPackageName, null)

            if (savedConfig != null) {
                val parts = savedConfig.split("|")
                if (parts.size >= 4) {
                    val message = parts[0]
                    val useNotification = parts[1].toBoolean()
                    val useVoice = parts[2].toBoolean()
                    val audioUriString = parts[3]
                    
                    // استخراج الثواني
                    val cooldownSeconds = if (parts.size >= 5) parts[4].toLong() else 15L
                    val cooldownMillis = cooldownSeconds * 1000L

                    val currentTime = System.currentTimeMillis()
                    val lastTime = lastTriggerTimeMap[openedPackageName] ?: 0L

                    // 3. التحقق من الوقت عشان لو قفل الكيبورد ميشتغلش الصوت مرتين ورا بعض
                    if (currentTime - lastTime >= cooldownMillis) {
                        lastTriggerTimeMap[openedPackageName] = currentTime
                        triggerAlert(openedPackageName, message, useNotification, useVoice, audioUriString)
                    }

                    // 4. تشغيل المؤقت اللي هيكرر الصوت تلقائياً كل (X ثانية) بدون ما تضغط على أي حاجة
                    startReminder(openedPackageName, message, useNotification, useVoice, audioUriString, cooldownMillis)
                }
            }
        }
    }

    // دالة إطلاق التنبيه
    private fun triggerAlert(appName: String, message: String, useNotification: Boolean, useVoice: Boolean, uriString: String) {
        if (useNotification) {
            sendNotification(appName, message)
        }
        if (useVoice && uriString.isNotEmpty()) {
            playUserAudio(uriString)
        }
    }

    // دالة التكرار التلقائي (المؤقت الذكي)
    private fun startReminder(pkg: String, msg: String, useNotif: Boolean, useVoice: Boolean, uri: String, intervalMillis: Long) {
        reminderRunnable = object : Runnable {
            override fun run() {
                // يتأكد إنك لسه فاتح نفس التطبيق قبل ما يكرر
                if (activePackage == pkg) {
                    lastTriggerTimeMap[pkg] = System.currentTimeMillis()
                    triggerAlert(pkg, msg, useNotif, useVoice, uri)
                    
                    // يعيد تشغيل نفسه بعد نفس المدة المحددة
                    handler.postDelayed(this, intervalMillis)
                }
            }
        }
        handler.postDelayed(reminderRunnable!!, intervalMillis)
    }

    // دالة إيقاف المؤقت التلقائي
    private fun stopReminder() {
        reminderRunnable?.let { handler.removeCallbacks(it) }
        reminderRunnable = null
    }

    private fun playUserAudio(uriString: String) {
        try {
            mediaPlayer?.release()
            val audioUri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer.create(this, audioUri)
            
            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            
            mediaPlayer?.start()
        } catch (e: Exception) {
            // تجاهل الخطأ
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
        stopReminder()
        mediaPlayer?.release()
        super.onDestroy()
    }
}

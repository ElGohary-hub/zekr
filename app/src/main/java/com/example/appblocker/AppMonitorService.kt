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
                    // استخراج نوع الصوت، ولو البيانات قديمة بنعتبره أنثى كافتراضي
                    val isMale = if (parts.size >= 4) parts[3].toBoolean() else false

                    // نمرر اختيار الصوت للدالة
                    if (useVoice) speakText(message, isMale)
                    if (useNotification) sendNotification(openedPackageName, message)
                }
            }
        }
    }

    private fun speakText(text: String, isMale: Boolean) {
        if (tts == null) return

        try {
            // جلب كل الأصوات المتاحة في موبايلك
            val availableVoices = tts?.voices
            if (availableVoices != null) {
                // تصفية الأصوات عشان نجيب العربي بس
                val arabicVoices = availableVoices.filter { it.locale.language.startsWith("ar") }
                
                if (arabicVoices.isNotEmpty()) {
                    // محاولة البحث عن كلمة male أو female في اسم الصوت
                    val genderKeyword = if (isMale) "male" else "female"
                    var selectedVoice = arabicVoices.find { it.name.contains(genderKeyword, ignoreCase = true) }
                    
                    // لو ملوناش الكلمة صريحة، بناخد أول صوت للأنثى وآخر صوت للذكر كحل بديل
                    if (selectedVoice == null) {
                        selectedVoice = if (isMale) arabicVoices.last() else arabicVoices.first()
                    }
                    
                    tts?.voice = selectedVoice
                }
            }
        } catch (e: Exception) {
            // تجاهل الخطأ لو حصل مشكلة في جلب الأصوات
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ar") // اللغة الأساسية
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}

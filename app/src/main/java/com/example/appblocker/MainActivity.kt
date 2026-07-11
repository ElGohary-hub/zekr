package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // قائمة لتخزين بيانات التطبيقات
    private val appList = mutableListOf<AppInfo>()

    // قالب لحفظ اسم التطبيق وحزمته
    data class AppInfo(val name: String, val packageName: String) {
        override fun toString(): String {
            return name // ده الاسم اللي هيظهر للمستخدم في القائمة
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOpenSettings = findViewById<Button>(R.id.btnOpenSettings)
        val spinnerApps = findViewById<Spinner>(R.id.spinnerApps)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val cbVoice = findViewById<CheckBox>(R.id.cbVoice)
        val cbNotification = findViewById<CheckBox>(R.id.cbNotification)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val sharedPref = getSharedPreferences("AppConfigs", Context.MODE_PRIVATE)

        btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // جلب التطبيقات المثبتة
        loadInstalledApps()

        // ربط التطبيقات بالقائمة المنسدلة
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, appList)
        spinnerApps.adapter = adapter

        // حفظ الإعدادات لما المستخدم يضغط على الزرار
        btnSave.setOnClickListener {
            val selectedApp = spinnerApps.selectedItem as? AppInfo
            val message = etMessage.text.toString().trim()
            val useVoice = cbVoice.isChecked
            val useNotification = cbNotification.isChecked

            // التأكد إن المستخدم اختار على الأقل صوت أو إشعار
            if (!useVoice && !useNotification) {
                Toast.makeText(this, "يجب تفعيل الصوت أو الإشعار على الأقل!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedApp != null && message.isNotEmpty()) {
                val editor = sharedPref.edit()
                val configData = "$message|$useVoice|$useNotification"
                
                // بنحفظ الإعدادات بناءً على حزمة التطبيق المخفية اللي اختاره
                editor.putString(selectedApp.packageName, configData)
                editor.apply()
                
                Toast.makeText(this, "تم الحفظ بنجاح لتطبيق ${selectedApp.name}", Toast.LENGTH_LONG).show()
                etMessage.text.clear() // تفريغ الخانة بعد الحفظ
            } else {
                Toast.makeText(this, "الرجاء كتابة الرسالة التحذيرية", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // دالة لجلب التطبيقات وعرضها
    private fun loadInstalledApps() {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in packages) {
            // بنفلتر التطبيقات اللي ينفع تتفتح بس (عشان نستبعد ملفات النظام)
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                appList.add(AppInfo(appName, appInfo.packageName))
            }
        }
        // ترتيب التطبيقات أبجدياً عشان المستخدم يسهل عليه البحث
        appList.sortBy { it.name }
    }
}

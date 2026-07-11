package com.example.appblocker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val appList = mutableListOf<AppInfo>()
    private lateinit var btnOpenSettings: Button // عرفنا الزرار هنا عشان نتحكم فيه في الشاشة كلها

    data class AppInfo(val name: String, val packageName: String) {
        override fun toString(): String {
            return name
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnOpenSettings = findViewById(R.id.btnOpenSettings)
        val spinnerApps = findViewById<Spinner>(R.id.spinnerApps)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val cbVoice = findViewById<CheckBox>(R.id.cbVoice)
        val cbNotification = findViewById<CheckBox>(R.id.cbNotification)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val sharedPref = getSharedPreferences("AppConfigs", Context.MODE_PRIVATE)

        // ===== التعديل الأول: طلب إذن الإشعارات لأندرويد 13 فما فوق =====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        loadInstalledApps()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, appList)
        spinnerApps.adapter = adapter

        btnSave.setOnClickListener {
            val selectedApp = spinnerApps.selectedItem as? AppInfo
            val message = etMessage.text.toString().trim()
            val useVoice = cbVoice.isChecked
            val useNotification = cbNotification.isChecked

            if (!useVoice && !useNotification) {
                Toast.makeText(this, "يجب تفعيل الصوت أو الإشعار على الأقل!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedApp != null && message.isNotEmpty()) {
                val editor = sharedPref.edit()
                val configData = "$message|$useVoice|$useNotification"
                
                editor.putString(selectedApp.packageName, configData)
                editor.apply()
                
                Toast.makeText(this, "تم الحفظ بنجاح لتطبيق ${selectedApp.name}", Toast.LENGTH_LONG).show()
                etMessage.text.clear()
            } else {
                Toast.makeText(this, "الرجاء كتابة الرسالة التحذيرية", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ===== التعديل الثاني: دالة تعمل كل مرة الشاشة تظهر للمستخدم =====
    override fun onResume() {
        super.onResume()
        if (isAccessibilityEnabled()) {
            btnOpenSettings.visibility = View.GONE // إخفاء الزرار
        } else {
            btnOpenSettings.visibility = View.VISIBLE // إظهار الزرار
        }
    }

    // دالة للتحقق هل صلاحية الوصول مفعلة لتطبيقنا ولا لأ
    private fun isAccessibilityEnabled(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains(packageName) == true
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in packages) {
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                appList.add(AppInfo(appName, appInfo.packageName))
            }
        }
        appList.sortBy { it.name }
    }
}

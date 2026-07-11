package com.example.appblocker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val appList = mutableListOf<AppInfo>()
    private lateinit var btnOpenSettings: Button
    private var selectedAudioUri: String = "" 

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
        val etCooldown = findViewById<EditText>(R.id.etCooldown) // الخانة الجديدة
        val cbVoice = findViewById<CheckBox>(R.id.cbVoice)
        val cbNotification = findViewById<CheckBox>(R.id.cbNotification)
        val btnPickAudio = findViewById<Button>(R.id.btnPickAudio)
        val tvAudioStatus = findViewById<TextView>(R.id.tvAudioStatus)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val sharedPref = getSharedPreferences("AppConfigs", Context.MODE_PRIVATE)

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

        val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    selectedAudioUri = uri.toString()
                    tvAudioStatus.text = "تم اختيار الملف بنجاح ✅"
                    tvAudioStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")) 
                } catch (e: SecurityException) {
                    Toast.makeText(this, "حدث خطأ في صلاحية الملف، يرجى اختيار ملف آخر", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnPickAudio.setOnClickListener {
            pickAudioLauncher.launch(arrayOf("audio/*")) 
        }

        btnSave.setOnClickListener {
            val selectedApp = spinnerApps.selectedItem as? AppInfo
            val message = etMessage.text.toString().trim()
            val cooldownStr = etCooldown.text.toString().trim() // قراءة المدة
            val useVoice = cbVoice.isChecked
            val useNotification = cbNotification.isChecked

            // لو المستخدم مكتبش رقم، هنعتبرها 15 ثانية افتراضياً
            val cooldownSeconds = if (cooldownStr.isNotEmpty()) cooldownStr.toInt() else 15

            if (!useVoice && !useNotification) {
                Toast.makeText(this, "يجب تفعيل الصوت أو الإشعار على الأقل!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (useVoice && selectedAudioUri.isEmpty()) {
                Toast.makeText(this, "الرجاء اختيار ملف صوتي من هاتفك!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedApp != null && message.isNotEmpty()) {
                val editor = sharedPref.edit()
                // إضافة الرقم الجديد للبيانات المحفوظة
                val configData = "$message|$useNotification|$useVoice|$selectedAudioUri|$cooldownSeconds"
                
                editor.putString(selectedApp.packageName, configData)
                editor.apply()
                
                Toast.makeText(this, "تم الحفظ بنجاح لتطبيق ${selectedApp.name}", Toast.LENGTH_LONG).show()
                
                etMessage.text.clear()
                etCooldown.text.clear()
                selectedAudioUri = ""
                tvAudioStatus.text = "لم يتم اختيار ملف"
                tvAudioStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
            } else {
                Toast.makeText(this, "الرجاء كتابة الرسالة التحذيرية", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityEnabled()) {
            btnOpenSettings.visibility = View.GONE
        } else {
            btnOpenSettings.visibility = View.VISIBLE
        }
    }

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

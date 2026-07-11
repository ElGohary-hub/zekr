package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOpenSettings = findViewById<Button>(R.id.btnOpenSettings)
        val etPackageName = findViewById<EditText>(R.id.etPackageName)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val cbVoice = findViewById<CheckBox>(R.id.cbVoice)
        val cbNotification = findViewById<CheckBox>(R.id.cbNotification)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val sharedPref = getSharedPreferences("AppConfigs", Context.MODE_PRIVATE)

        btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnSave.setOnClickListener {
            val packageName = etPackageName.text.toString().trim()
            val message = etMessage.text.toString().trim()
            val useVoice = cbVoice.isChecked
            val useNotification = cbNotification.isChecked

            if (packageName.isNotEmpty() && message.isNotEmpty()) {
                val editor = sharedPref.edit()
                val configData = "$message|$useVoice|$useNotification"
                editor.putString(packageName, configData)
                editor.apply()
                Toast.makeText(this, "تم الحفظ بنجاح!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "الرجاء إدخال البيانات", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

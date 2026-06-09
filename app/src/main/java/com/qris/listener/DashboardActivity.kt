package com.qris.listener

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.button.MaterialButton

class DashboardActivity : AppCompatActivity() {

    private lateinit var statusBanner: TextView
    private lateinit var iconNetwork: TextView
    private lateinit var iconNotification: TextView
    private lateinit var iconLogin: TextView
    private lateinit var iconListener: TextView
    private lateinit var iconBattery: TextView
    private lateinit var infoText: TextView
    private lateinit var accountName: TextView
    private lateinit var actionNotification: TextView
    private lateinit var actionBattery: TextView
    private lateinit var descBattery: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        statusBanner = findViewById(R.id.statusBanner)
        iconNetwork = findViewById(R.id.iconNetwork)
        iconNotification = findViewById(R.id.iconNotification)
        iconLogin = findViewById(R.id.iconLogin)
        iconListener = findViewById(R.id.iconListener)
        iconBattery = findViewById(R.id.iconBattery)
        infoText = findViewById(R.id.infoText)
        accountName = findViewById(R.id.accountName)
        actionNotification = findViewById(R.id.actionNotification)
        actionBattery = findViewById(R.id.actionBattery)
        descBattery = findViewById(R.id.descBattery)

        val prefs = getSharedPreferences("qris_session", Context.MODE_PRIVATE)
        val name = prefs.getString("user_name", "")
        val email = prefs.getString("user_email", "")
        accountName.text = if (!name.isNullOrEmpty()) name else email ?: ""

        findViewById<MaterialButton>(R.id.logoutButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Yakin ingin keluar?")
                .setPositiveButton("Ya") { _, _ -> doLogout() }
                .setNegativeButton("Batal", null)
                .show()
        }

        findViewById<View>(R.id.reqNotification).setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        findViewById<View>(R.id.reqBattery).setOnClickListener {
            if (!isBatteryOptDisabled()) {
                requestBatteryOptimization()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAllRequirements()
    }

    private fun checkAllRequirements() {
        val hasInternet = checkInternet()
        val hasNotif = isNotificationServiceEnabled()
        val hasSession = getSharedPreferences("qris_session", Context.MODE_PRIVATE)
            .contains("session_token")
        val hasListener = isListenerActive()
        val hasBattery = isBatteryOptDisabled()

        setRequirement(iconNetwork, hasInternet)
        setRequirement(iconNotification, hasNotif)
        setRequirement(iconLogin, hasSession)
        setRequirement(iconListener, hasListener)
        setRequirement(iconBattery, hasBattery)

        actionNotification.visibility = if (!hasNotif) View.VISIBLE else View.GONE
        actionBattery.visibility = if (!hasBattery) View.VISIBLE else View.GONE

        if (!hasBattery) {
            descBattery.text = "Ketuk untuk nonaktifkan optimasi"
        } else {
            descBattery.text = getString(R.string.req_battery_desc)
        }

        val allOk = hasInternet && hasNotif && hasSession && hasListener && hasBattery
        if (allOk) {
            statusBanner.text = getString(R.string.status_aman)
            statusBanner.setBackgroundResource(R.drawable.bg_banner_safe)
            infoText.text = getString(R.string.info_all_ok)
        } else {
            statusBanner.text = getString(R.string.status_tidak_aman)
            statusBanner.setBackgroundResource(R.drawable.bg_banner_unsafe)
            infoText.text = buildString {
                append("Lengkapi berikut:\n")
                if (!hasInternet) append("\n• Koneksi Internet")
                if (!hasNotif) append("\n• Izin Notifikasi — ketuk untuk atur")
                if (!hasSession) append("\n• Session Login — login ulang")
                if (!hasListener) append("\n• Layanan Listener")
                if (!hasBattery) append("\n• Optimasi Baterai — ketuk untuk atur")
                append("\n\nSetelah semua terpenuhi, status akan berubah menjadi AMAN.")
            }
        }
    }

    private fun setRequirement(view: TextView, ok: Boolean) {
        if (ok) {
            view.text = "\u2714\uFE0F"
            view.setTextColor(getColor(R.color.green))
        } else {
            view.text = "\u274C"
            view.setTextColor(getColor(R.color.red))
        }
    }

    private fun checkInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packageNames.contains(packageName)
    }

    private fun isListenerActive(): Boolean {
        val cn = ComponentName(this, PaymentNotificationListener::class.java)
        val enabled = try {
            packageManager.getComponentEnabledSetting(cn)
        } catch (_: Exception) {
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }
        if (enabled == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) return false
        return isNotificationServiceEnabled()
    }

    private fun isBatteryOptDisabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun doLogout() {
        getSharedPreferences("qris_session", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

package com.qris.listener

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class PaymentNotificationListener : NotificationListenerService() {

    private val TARGET_PACKAGES = setOf(
        "id.bmri.livinmerchant",
        "com.gojek.gopaymerchant"
    )

    private val KEYWORDS = setOf(
        "berhasil", "masuk", "diterima", "pembayaran",
        "success", "received", "payment", "credit"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (sbn.packageName !in TARGET_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        val match = KEYWORDS.any { text.contains(it, ignoreCase = true) }
        if (!match) return

        Log.d("QRISListener", "[${sbn.packageName}] $title | $text")
        sendToBackend(sbn.packageName, title, text)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun sendToBackend(app: String, title: String, text: String) {
        thread(start = true) {
            var connection: HttpURLConnection? = null
            try {
                val prefs = applicationContext.getSharedPreferences("qris_session", Context.MODE_PRIVATE)
                val session = prefs.getString("session_token", "") ?: ""

                val url = URL("https://qris.yamitra.com/webhook/v1")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val payload = JSONObject().apply {
                    put("app", app)
                    put("title", title)
                    put("text", text)
                    put("session", session)
                }

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                Log.d("QRISListener", "Webhook sent, Response: ${connection.responseCode}")
            } catch (e: Exception) {
                Log.e("QRISListener", "Gagal mengirim webhook", e)
            } finally {
                connection?.disconnect()
            }
        }
    }
}

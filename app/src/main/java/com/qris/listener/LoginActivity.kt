package com.qris.listener

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var errorText: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        errorText = findViewById(R.id.errorText)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showError("Email dan kata sandi harus diisi")
                return@setOnClickListener
            }

            doLogin(email, password)
        }
    }

    private fun doLogin(email: String, password: String) {
        setLoading(true)
        errorText.visibility = android.view.View.GONE

        thread(start = true) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://yamitra.com/customer/loginWithApp")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseBytes = if (responseCode in 200..299) {
                    connection.inputStream.readBytes()
                } else {
                    connection.errorStream?.readBytes() ?: ByteArray(0)
                }
                val responseBody = String(responseBytes, Charsets.UTF_8)

                runOnUiThread {
                    setLoading(false)
                    if (responseCode in 200..299) {
                        handleLoginSuccess(responseBody)
                    } else {
                        handleLoginError(responseBody)
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    setLoading(false)
                    showError("Gagal terhubung ke server: ${e.localizedMessage}")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun handleLoginSuccess(response: String) {
        try {
            val json = JSONObject(response)
            val session = json.optString("session", null)
            val status = json.optString("status", "")

            if (status == "success" && session != null) {
                val name = json.optString("name", emailInput.text.toString().trim().substringBefore("@"))
                getSharedPreferences("qris_session", Context.MODE_PRIVATE)
                    .edit()
                    .putString("session_token", session)
                    .putString("user_email", emailInput.text.toString().trim())
                    .putString("user_name", name)
                    .apply()

                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                val msg = json.optString("message", "Email atau kata sandi salah")
                showError(msg)
            }
        } catch (e: Exception) {
            showError("Gagal memproses respons server")
        }
    }

    private fun handleLoginError(response: String) {
        try {
            val json = JSONObject(response)
            val msg = json.optString("message", "Email atau kata sandi salah")
            showError(msg)
        } catch (_: Exception) {
            showError("Email atau kata sandi salah")
        }
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        loginButton.text = if (loading) getString(R.string.login_loading) else getString(R.string.login_button)
        emailInput.isEnabled = !loading
        passwordInput.isEnabled = !loading
    }

    private fun showError(msg: String) {
        errorText.text = msg
        errorText.visibility = android.view.View.VISIBLE
    }
}

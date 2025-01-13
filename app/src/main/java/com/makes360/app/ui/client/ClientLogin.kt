package com.makes360.app.ui.client

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.makes360.app.databinding.ActivityClientLoginBinding
import com.makes360.app.R
import org.json.JSONObject
import java.util.Calendar

class ClientLogin : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var loginButton: Button
    private lateinit var progressOverlay: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var mBinding: ActivityClientLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check login state before setting content view or initializing views
        if (isLoggedIn()) {
            navigateToClientDetail()
            return
        }

        // Initialize binding
        mBinding = ActivityClientLoginBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Initialize views and listeners
        initializeViews()
        setClickListeners()
        footer()
    }

    private fun initializeViews() {
        emailEditText = findViewById(R.id.emailEditText)
        otpEditText = findViewById(R.id.otpEditText)
        sendOtpButton = findViewById(R.id.sendOtpButton)
        loginButton = findViewById(R.id.loginButton)
        progressOverlay = findViewById(R.id.progressOverlay)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setClickListeners() {
        sendOtpButton.setOnClickListener {
            emailEditText.clearFocus()
            hideKeyboard(emailEditText)
            sendOtp()

        }

        loginButton.setOnClickListener {
            otpEditText.clearFocus()
            hideKeyboard(otpEditText)
            verifyOtp()
        }
        mBinding.applyLinkBtn.setOnClickListener { visitOurWebsite() }
    }

    private fun footer() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        mBinding.footerTextView.text = "$currentYear © AGI Innovations Makes360 Private Limited"
    }

    // Utility function to hide the keyboard
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    private fun saveLoginState(email: String) {
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("client_email", email)
        editor.apply()
    }

    // Utility function to check login state
    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("client_email", null) != null
    }

    // Utility function to navigate to Client Detail activity
    private fun navigateToClientDetail() {
        val email = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
            .getString("client_email", null)
        val intent = Intent(this, ClientDashboard::class.java)
        intent.putExtra("EMAIL", email)
        startActivity(intent)
        finish()
    }


    private fun visitOurWebsite() {
        val url = "https://www.makes360.com/"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        startActivity(intent)
    }

    private fun showLoader() {
        progressBar.visibility = View.VISIBLE
        progressOverlay.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressBar.visibility = View.GONE
        progressOverlay.visibility = View.GONE
    }

    // Function to send OTP
    private var sentOtp: String? = null // Store the sent OTP

    private fun sendOtp() {
        val email = emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://www.makes360.com/application/makes360/client/client_send_otp.php"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        showLoader()

        val stringRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                hideLoader()
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optBoolean("success", false)) {
                        sentOtp = jsonResponse.optString("otp")
                        Toast.makeText(this, "OTP sent successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = jsonResponse.optString("error", "Unknown error occurred")
                        Toast.makeText(this, "Failed: $error", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            Response.ErrorListener { error ->
                hideLoader()
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }

            override fun getBody(): ByteArray {
                val params = mapOf("email" to email)
                return JSONObject(params).toString().toByteArray()
            }
        }

        requestQueue.add(stringRequest)
    }

    // Function to verify OTP
    private fun verifyOtp() {
        val enteredOtp = otpEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
            return
        }

        if (sentOtp == null) {
            Toast.makeText(this, "Please request an OTP first.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://www.makes360.com/application/makes360/client/client_verify_otp.php"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optBoolean("success", false)) {
                        saveLoginState(email)
                        val intent = Intent(this, ClientDashboard::class.java)
                        intent.putExtra("EMAIL", email)
                        Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT)
                            .show()
                        startActivity(intent)
                        finish()
                    } else {
                        val error = jsonResponse.optString("error", "Invalid OTP")
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }

            override fun getBody(): ByteArray {
                val params = mapOf(
                    "sentOtp" to sentOtp,
                    "inputOtp" to enteredOtp,
                    "email" to email
                )
                return JSONObject(params).toString().toByteArray()
            }
        }

        requestQueue.add(stringRequest)
    }
}
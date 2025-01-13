package com.makes360.app.ui.intern

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
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.makes360.app.databinding.ActivityInternLoginBinding
import com.makes360.app.BaseActivity
import com.makes360.app.R
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

class InternLogin : BaseActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var loginButton: Button
    private lateinit var progressOverlay: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var mBinding: ActivityInternLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check login state before setting content view or initializing views
        if (isLoggedIn()) {
            navigateToInternTrack()
            return
        }

        // Initialize binding
        mBinding = ActivityInternLoginBinding.inflate(layoutInflater)
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
        mBinding.applyLinkBtn.setOnClickListener { applyForInternship() }
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
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("user_email", email)
        editor.apply()
    }

    // Utility function to check login state
    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("user_email", null) != null
    }

    // Utility function to navigate to InternTrack activity
    private fun navigateToInternTrack() {
        val email = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
            .getString("user_email", null)
        val intent = Intent(this, InternDashboard::class.java)
        intent.putExtra("EMAIL", email)
        startActivity(intent)
        finish()
    }


    private fun applyForInternship() {
//        if (!checkInternetConnection()) {
//            hideLoader()
//            showToast("No internet connection. Please check your connection.")
//            return
//        }

        val url = "https://www.google.com/search?q=internship+makes360"
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


    private var isOtpRequestInProgress = false

    private fun sendOtp() {
        if (isOtpRequestInProgress) {
            Toast.makeText(
                this,
                "OTP request already in progress. Please wait...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

//        if (!checkInternetConnection()) {
//            showToast("No internet connection. Please check your connection.")
//            return
//        }

        val email = emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        isOtpRequestInProgress = true
        sendOtpButton.isEnabled = false // Disable button
        showLoader() // Show loader

        val url = "https://www.makes360.com/application/makes360/internship/send_otp.php"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val jsonBody = JSONObject().apply {
            put("email", email)
        }

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { response ->
                isOtpRequestInProgress = false
                sendOtpButton.isEnabled = true // Re-enable button
                hideLoader() // Hide loader

                try {
                    val success = response.optBoolean("success", false)
                    val message = response.optString("error", "Unknown response from server")

                    if (success) {
                        Toast.makeText(this, "OTP sent successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Display the specific message returned from the server
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(this, "Response parsing error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            { error ->
                isOtpRequestInProgress = false
                sendOtpButton.isEnabled = true // Re-enable button
                hideLoader() // Hide loader
                Toast.makeText(
                    this,
                    "Network error: ${error.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Content-Type" to "application/json")
            }
        }

        requestQueue.add(jsonObjectRequest)
    }


    // Function to verify OTP
    private fun verifyOtp() {
//        if (!checkInternetConnection()) {
//            showToast("No internet connection. Please check and try again.")
//            return
//        }

        val enteredOtp = otpEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
            return
        }

        showLoader() // Show loader

        val url = "https://www.makes360.com/application/makes360/internship/verify_otp.php"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("inputOtp", enteredOtp)
        }

        val stringRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                hideLoader() // Hide loader
                try {
                    val jsonResponse = JSONObject(response)
                    val message = jsonResponse.optString("message", "Operation completed")
                    val error = jsonResponse.optString("error", "")

                    if (jsonResponse.optBoolean("success", false)) {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        saveLoginState(email)
                        startActivity(
                            Intent(this, InternDashboard::class.java).putExtra(
                                "EMAIL",
                                email
                            )
                        )
                        finish()
                    } else {
                        Toast.makeText(this, error.ifEmpty { message }, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(this, "Response parsing error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            Response.ErrorListener { error ->
                hideLoader() // Hide loader
                val errorMessage = error.message ?: "Unknown network error"
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getBody(): ByteArray {
                return jsonBody.toString().toByteArray(Charsets.UTF_8)
            }

            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Content-Type" to "application/json")
            }
        }

        requestQueue.add(stringRequest)
    }


    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
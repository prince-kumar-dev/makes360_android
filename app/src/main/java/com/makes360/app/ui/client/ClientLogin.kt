package com.makes360.app.ui.client

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.databinding.ActivityClientLoginBinding
import com.makes360.app.util.NetworkUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.Executor

class ClientLogin : BaseActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var loginButton: Button
    private lateinit var progressOverlay: FrameLayout
    private lateinit var progressBar: LottieAnimationView
    private lateinit var mBinding: ActivityClientLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var sharedPreferences: SharedPreferences

    private val AUTH_TIMEOUT = 60 * 1000 // 30 seconds in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Check login state before setting content view or initializing views
        if (isLoggedIn()) {
            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

            // Check if authentication is needed
            if (isAuthenticationRequired()) {
                setupBiometricAuthentication()
            } else {
                navigateToClientPanel() // Directly load credentials if within the timeout period
            }
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

    private fun isAuthenticationRequired(): Boolean {
        val lastAuthTime = sharedPreferences.getLong("last_auth_time", 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastAuthTime) > AUTH_TIMEOUT
    }

    private fun saveAuthenticationTime() {
        sharedPreferences.edit().putLong("last_auth_time", System.currentTimeMillis()).apply()
    }

    private fun setupBiometricAuthentication() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt()
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(
                    this,
                    "Biometric authentication not available. Use device credentials.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    saveAuthenticationTime() // Save authentication time on success
                    Toast.makeText(
                        applicationContext,
                        "Authentication Successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToClientPanel()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext,
                        "Authentication Error: $errString",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication Failed", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to Access Panel")
            .setSubtitle("Use fingerprint or device credentials to continue")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
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
        mBinding.footerTextView.text = getString(R.string.footer_text, currentYear)
    }

    // Utility function to hide the keyboard
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    private fun saveLoginState(
        email: String,
        custId: String,
        firstName: String,
        gender: String,
        profilePic: String
    ) {
        sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("client_email", email)
        editor.putString("client_cust_id", custId) // Save custId
        editor.putString("client_first_name", firstName)
        editor.putString("client_gender", gender)
        editor.putString("client_profile_pic", profilePic)
        editor.apply()
    }

    // Utility function to check login state
    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("client_email", null) != null
    }

    // Utility function to retrieve saved custId
    private fun getCustId(): String? {
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("client_cust_id", null)
    }

    private fun getFirstName(): String? {
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("client_first_name", null)
    }

    private fun getGender(): String? {
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("client_gender", null)
    }

    private fun getProfilePic(): String? {
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("client_profile_pic", null)
    }


    // Utility function to navigate to Client Panel activity
    private fun navigateToClientPanel() {
        val email = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
            .getString("client_email", null)
        val custId = getCustId()
        val firstName = getFirstName()
        val gender = getGender()
        val profilePic = getProfilePic()

        val intent = Intent(this, ClientDashboard::class.java)
        intent.putExtra("EMAIL", email)
        intent.putExtra("CUST_ID", custId)
        intent.putExtra("FIRST_NAME", firstName)
        intent.putExtra("GENDER", gender)
        intent.putExtra("PROFILE_PIC", profilePic)
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
        progressBar.playAnimation()
        progressOverlay.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressBar.cancelAnimation()
        progressOverlay.visibility = View.GONE
    }

    private var isOtpRequestInProgress = false
    private var cooldownTimer: CountDownTimer? = null

    private fun sendOtp() {
        if (isOtpRequestInProgress) {
            Toast.makeText(
                this,
                "OTP request already in progress. Please wait...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val email = emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        isOtpRequestInProgress = true
        sendOtpButton.isEnabled = false // Disable button
        sendOtpButton.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
        showLoader() // Show loader

        val url = "https://www.makes360.com/application/makes360/client/send-otp.php"
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
                hideLoader() // Hide loader

                try {
                    val success = response.optBoolean("success", false)
                    val message = response.optString("error", "Unknown response from server")
                    val retryAfter = response.optInt("retry_after", 0)

                    if (success) {
                        Toast.makeText(this, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                        startCooldownTimer(60) // 60 seconds cooldown
                    } else if (retryAfter > 0) {
                        // Handle server-side cooldown
                        Toast.makeText(
                            this,
                            "Please wait $retryAfter seconds before requesting again",
                            Toast.LENGTH_LONG
                        ).show()
                        startCooldownTimer(retryAfter)
                    } else {
                        // Generic error message
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        sendOtpButton.isEnabled = true // Re-enable button
                        sendOtpButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                    }
                } catch (e: JSONException) {
                    Toast.makeText(this, "Response parsing error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    sendOtpButton.isEnabled = true // Re-enable button
                    sendOtpButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
            },
            { error ->
                isOtpRequestInProgress = false
                hideLoader() // Hide loader
                sendOtpButton.isEnabled = true // Re-enable button
                sendOtpButton.setTextColor(ContextCompat.getColor(this, R.color.white))

                val statusCode = error.networkResponse?.statusCode
                if (statusCode == 429) {
                    Toast.makeText(
                        this,
                        "Too many requests. Please try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Network error: ${error.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Content-Type" to "application/json")
            }
        }

        requestQueue.add(jsonObjectRequest)
    }

    private fun startCooldownTimer(seconds: Int) {
        cooldownTimer?.cancel() // Cancel any existing timer
        cooldownTimer = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = millisUntilFinished / 1000
                sendOtpButton.text = "Wait ${remainingSeconds}s"
            }

            override fun onFinish() {
                sendOtpButton.isEnabled = true
                sendOtpButton.text = getString(R.string.send_otp)
            }
        }.apply { start() }
    }

    // Function to verify OTP
    private fun verifyOtp() {
        val enteredOtp = otpEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
            return
        }

        showLoader() // Show loader

        val url = "https://www.makes360.com/application/makes360/client/verify-otp.php"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optBoolean("success", false)) {
                        val custId = jsonResponse.optString("cust_id", null.toString())
                        val firstName = jsonResponse.optString("first_name", null.toString())
                        val gender = jsonResponse.optString("gender", null.toString())
                        val profilePic = jsonResponse.optString("profile_pic", null.toString())
                        saveLoginState(email, custId, firstName, gender, profilePic)
                        val intent = Intent(this, ClientDashboard::class.java)
                        intent.putExtra("EMAIL", email)
                        intent.putExtra("CUST_ID", custId)
                        intent.putExtra("FIRST_NAME", firstName)
                        intent.putExtra("GENDER", gender)
                        intent.putExtra("PROFILE_PIC", profilePic)
                        Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show()
                        startActivity(intent)
                        finish()
                    } else {
                        hideLoader() // Hide loader
                        val error = jsonResponse.optString("error", "Invalid OTP")
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    hideLoader() // Hide loader
                    Toast.makeText(this, "Error parsing response: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            Response.ErrorListener { error ->
                hideLoader() // Hide loader
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
                    "email" to email,
                    "inputOtp" to enteredOtp
                )
                return JSONObject(params).toString().toByteArray()
            }
        }

        requestQueue.add(stringRequest)
    }
}
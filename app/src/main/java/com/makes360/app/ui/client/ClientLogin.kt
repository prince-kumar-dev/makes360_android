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

class ClientLogin : BaseActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var loginButton: Button
    private lateinit var progressOverlay: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var mBinding: ActivityClientLoginBinding

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
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
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


    // Utility function to navigate to Client Detail activity
    private fun navigateToClientDetail() {
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

        val email = emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        isOtpRequestInProgress = true
        sendOtpButton.isEnabled = false // Disable button
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
                    hideLoader()
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
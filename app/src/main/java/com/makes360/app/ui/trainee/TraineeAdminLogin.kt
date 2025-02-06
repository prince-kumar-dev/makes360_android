package com.makes360.app.ui.trainee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.makes360.app.BaseActivity
import com.makes360.app.databinding.ActivityTraineeAdminLoginBinding
import com.makes360.app.util.NetworkUtils
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread

class TraineeAdminLogin : BaseActivity() {

    private lateinit var mBinding: ActivityTraineeAdminLoginBinding
    private val loginPref = "AdminLoginPref"
    private val loginTimeKey = "AdminLoginTime"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Check if user is already logged in and session is valid
        if (isLoggedIn()) {
            // If logged in, proceed to next activity
            navigateToDashboard()
        }

        // Initialize binding
        mBinding = ActivityTraineeAdminLoginBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setClickListeners()
        footer()


        // Handle login button click
        mBinding.loginButton.setOnClickListener {
            val email = mBinding.emailEditText.text.toString()
            val password = mBinding.passwordEditText.text.toString()

            showLoader()

            // Fetch credentials from PHP and check if they match
            fetchCredentialsAndValidate(email, password)
        }
    }

    private fun setClickListeners() {
        mBinding.loginButton.setOnClickListener {
            mBinding.passwordEditText.clearFocus()
            hideKeyboard(mBinding.passwordEditText)
        }
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

    private fun showLoader() {
        mBinding.progressBar.visibility = View.VISIBLE
        mBinding.progressBar.playAnimation()
        mBinding.progressOverlay.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        mBinding.progressBar.cancelAnimation()
        mBinding.progressOverlay.visibility = View.GONE
    }

    private fun fetchCredentialsAndValidate(inputEmail: String, inputPassword: String) {
        thread {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/trainee/admin-credentials.php")
                val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connect()

                val inputStream = urlConnection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }

                val jsonResponse = JSONObject(response)
                val savedEmail = jsonResponse.getString("email")
                val savedPassword = jsonResponse.getString("password")

                runOnUiThread {
                    if (inputEmail == savedEmail && inputPassword == savedPassword) {
                        saveLoginTime()
                        navigateToDashboard()
                        hideLoader()
                    } else {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                        hideLoader()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                hideLoader()
            }
        }
    }

    private fun saveLoginTime() {
        val sharedPreferences = getSharedPreferences(loginPref, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentTime = System.currentTimeMillis()
        editor.putLong(loginTimeKey, currentTime)
        editor.apply()
    }

    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences(loginPref, MODE_PRIVATE)
        val loginTime = sharedPreferences.getLong(loginTimeKey, 0)

        if (loginTime == 0L) {
            return false
        }

        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - loginTime

        // Check if 24 hours have passed
        return timeDiff <= 24 * 60 * 60 * 1000 // 24 hours in milliseconds
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, TraineeAdminDashboard::class.java)
        startActivity(intent)
        finish()
    }

}
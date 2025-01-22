package com.makes360.app.ui.trainee

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.client.ClientTraineeProfileAdapter
import com.makes360.app.databinding.ActivityTraineeProfileBinding
import com.makes360.app.models.client.ClientTraineeProfileCategory
import com.makes360.app.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class TraineeProfile : BaseActivity() {

    private lateinit var mBinding: ActivityTraineeProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityTraineeProfileBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")

        if (email != null) {
            showLoader()
            fetchTraineeProfile(email)
        } else {
            showToast("Invalid email! Please try again.")
        }

        mBinding.logOut.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("TraineeLoginPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Clear all stored data
        editor.apply()

        // Redirect to login screen
        val intent = Intent(this, TraineeLogin::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        Toast.makeText(this, "Log Out Successfully", Toast.LENGTH_SHORT).show()
        startActivity(intent)
    }

    private fun showLoader() {
        mBinding.progressOverlay.visibility = View.VISIBLE
        mBinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        mBinding.progressOverlay.visibility = View.GONE
        mBinding.progressBar.visibility = View.GONE
    }

    private fun profileCategory(details: TraineeProfileDetails) {
        val profileImageView = findViewById<ImageView>(R.id.traineeImg)

        // Adding Image to the profileImageView
        profileImageView.setImageResource(
            when (details.gender) {
                "0" -> R.drawable.intern_girl
                "1" -> R.drawable.intern_boy
                else -> R.drawable.ic_question_mark
            }
        )

        val dob = formatDate(details.dob)

        fun String?.orPlaceholder() = if (this.isNullOrEmpty()) "Will be Updated" else this

        val categories = listOf(
            ClientTraineeProfileCategory(
                "General",
                mapOf(
                    "Mother's Name:" to details.motherName.orPlaceholder(),
                    "Date of Birth:" to dob.orPlaceholder(),
                    "Gender:" to if (details.gender == "0") "Female" else "Male",
                ),
                icon = R.drawable.ic_general
            ),
            ClientTraineeProfileCategory(
                "Contact Details",
                mapOf(
                    "Phone No:" to details.phone.orPlaceholder(),
                    "Address:" to details.address.orPlaceholder()
                ),
                icon = R.drawable.ic_phone
            ),
            ClientTraineeProfileCategory(
                "Academic Information",
                mapOf(
                    "College:" to details.college.orPlaceholder(),
                    "Department:" to details.department.orPlaceholder(),
                    "Roll No:" to details.rollNo.orPlaceholder()
                ),
                icon = R.drawable.ic_academic
            )
        )

        // Setup RecyclerView
        mBinding.profileRecyclerView.layoutManager = LinearLayoutManager(this)
        mBinding.profileRecyclerView.adapter = ClientTraineeProfileAdapter(categories)
    }

    private fun formatDate(dateString: String?): String {
        return if (dateString.isNullOrEmpty() || dateString == "null") {
            "Will be Updated"
        } else {
            try {
                SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)!!
                )
            } catch (e: Exception) {
                "Invalid Date"
            }
        }
    }

    private fun fetchTraineeProfile(email: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/trainee/trainee-profile-details.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                // Write email to request body
                val requestBody = "email=$email"
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()
                outputStream.close()

                // Check response code
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val profileDetails = parseResponse(response)

                    // Update UI on the main thread
                    runOnUiThread {
                        hideLoader()
                        updateProfileUI(profileDetails)
                        profileCategory(profileDetails)
                    }
                } else {
                    runOnUiThread {
                        hideLoader()
                        showToast("Failed to fetch details. Please try again.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    hideLoader()
                    showToast("Error occurred: ${e.message}")
                }
            }
        }
    }

    private fun updateProfileUI(profileDetails: TraineeProfileDetails) {
        mBinding.traineeName.text = profileDetails.name
        mBinding.traineeEmail.text = intent.getStringExtra("EMAIL")

        val lastLoginTimestamp = profileDetails.lastLogin

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val date = inputFormat.parse(lastLoginTimestamp)

        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val formattedDate = date?.let { outputFormat.format(it) }

        mBinding.traineeLastLogin.text = ("Last Login: $formattedDate")
    }

    private fun parseResponse(response: String): TraineeProfileDetails {
        val jsonResponse = JSONObject(response)
        return TraineeProfileDetails(
            name = jsonResponse.optString("name", "Unknown"),
            offerLetter = jsonResponse.optString("offer_letter", "Unknown"),
            motherName = jsonResponse.optString("mother_name", "Unknown"),
            email = jsonResponse.optString("email", "Unknown"),
            phone = jsonResponse.optString("phone", "Unknown"),
            college = jsonResponse.optString("college", "Unknown"),
            department = jsonResponse.optString("department", "Unknown"),
            rollNo = jsonResponse.optString("roll_no", "Unknown"),
            dob = jsonResponse.optString("dob", "Unknown"),
            gender = jsonResponse.optString("gender", "Unknown"),
            address = jsonResponse.optString("address", "Unknown"),
            status = jsonResponse.optString("status", "Unknown"),
            txnId = jsonResponse.optString("txnId", "Unknown"),
            createdAt = jsonResponse.optString("created_at", "Unknown"),
            updated = jsonResponse.optString("updated", "Unknown"),
            lastLogin = jsonResponse.optString("lastLogin", "Unknown")
        )
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class TraineeProfileDetails(
        val name: String,
        val offerLetter: String,
        val motherName: String,
        val email: String,
        val phone: String,
        val college: String,
        val department: String,
        val rollNo: String,
        val dob: String,
        val gender: String,
        val address: String,
        val status: String,
        val txnId: String,
        val createdAt: String,
        val updated: String,
        val lastLogin: String
    )

}
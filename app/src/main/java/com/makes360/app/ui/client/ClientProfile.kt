package com.makes360.app.ui.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.client.ClientTraineeProfileAdapter
import com.makes360.app.databinding.ActivityClientProfileBinding
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

class ClientProfile : BaseActivity() {

    private lateinit var mBinding: ActivityClientProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityClientProfileBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")
        if (email != null) {
            showLoader()
            fetchClientProfile(email)
        } else {
            showToast("Invalid email! Please try again.")
        }

        mBinding.logOut.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("ClientLoginPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Clear all stored data
        editor.apply()

        // Redirect to login screen
        val intent = Intent(this, ClientLogin::class.java).apply {
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

    private fun profileCategory(details: ProfileDetails) {
        val profileImageView = findViewById<ImageView>(R.id.clientImg)

        // Adding Image to the profileImageView
        if (details.profilePic.isEmpty() || details.profilePic == "null") {
            if (details.gender == "0")
                profileImageView.setImageResource(R.drawable.ic_man_client)
            else
                profileImageView.setImageResource(R.drawable.ic_female_client)
        } else {
            val url =
                "https://www.makes360.com/internship/apply/file/profile_pic/" + details.profilePic

            // Using Glide with circle crop
            Glide.with(this)
                .load(url)
                .apply(RequestOptions.circleCropTransform()) // Apply circle crop transformation
                .placeholder(R.drawable.circular_background) // Optional placeholder
                .into(profileImageView)
        }

        val dob = formatDate(details.dob)
        val anniversaryDate = formatDate(details.anniversaryDate)

        fun String?.orPlaceholder() = if (this.isNullOrEmpty()) "Will be Updated" else this

        val categories = listOf(
            ClientTraineeProfileCategory(
                "General",
                mapOf(
                    "Date of Birth:" to dob.orPlaceholder(),
                    "Gender:" to if (details.gender == "0") "Male" else "Female",
                    "Anniversary Date:" to anniversaryDate.orPlaceholder()
                ),
                icon = R.drawable.ic_general
            ),
            ClientTraineeProfileCategory(
                "Contact Details",
                mapOf(
                    "Phone No:" to details.phoneNumber.orPlaceholder(),
                    "WhatsApp No:" to details.whatsappNumber.orPlaceholder(),
                    "Address:" to details.address.orPlaceholder()
                ),
                icon = R.drawable.ic_phone
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

    private fun fetchClientProfile(email: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/client/profile-details.php")
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
                        showToast("Failed to fetch profile details. Please try again.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    hideLoader()
                    showToast("Error occurred.")
                }
            }
        }
    }

    private fun parseResponse(response: String): ProfileDetails {
        val jsonResponse = JSONObject(response)
        return ProfileDetails(
            companyName = jsonResponse.optString("companyName", "Unknown"),
            firstName = jsonResponse.optString("firstName", "Unknown"),
            lastName = jsonResponse.optString("lastName", "Unknown"),
            gender = jsonResponse.optString("gender", "Unknown"),
            email = jsonResponse.optString("email", "Unknown"),
            profilePic = jsonResponse.optString("profilePic", "Unknown"),
            phoneNumber = jsonResponse.optString("phoneNo", "Unknown"),
            whatsappNumber = jsonResponse.optString("whatsappNo", "Unknown"),
            address = jsonResponse.optString("address", "Unknown"),
            dob = jsonResponse.optString("dob", "Unknown"),
            anniversaryDate = jsonResponse.optString("anniversaryDate", "Unknown")
        )
    }

    private fun updateProfileUI(details: ProfileDetails) {
        mBinding.clientName.text = "${details.firstName}  ${details.lastName}"
        mBinding.clientEmail.text = intent.getStringExtra("EMAIL")
        mBinding.clientCompanyTxtView.text = details.companyName
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class ProfileDetails(
        val companyName: String,
        val firstName: String,
        val lastName: String,
        val gender: String,
        val email: String,
        val profilePic: String,
        val phoneNumber: String,
        val whatsappNumber: String,
        val address: String,
        val dob: String,
        val anniversaryDate: String
    )

}
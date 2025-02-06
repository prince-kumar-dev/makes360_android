package com.makes360.app.ui.intern

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.databinding.ActivityInternProfileBinding
import com.makes360.app.adapters.InternProfileAdapter
import com.makes360.app.models.InternProfileCategory
import com.makes360.app.ui.client.ClientLogin
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

class InternProfile : BaseActivity() {

    private lateinit var mBinding: ActivityInternProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {

        // Initialize binding
        mBinding = ActivityInternProfileBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")
        if (email != null) {
            showLoader()
            fetchInternProfile(email)
        } else {
            showToast("Invalid email! Please try again.")
        }

        mBinding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (email != null) {
                    fetchInternProfile(email)
                }
            } else {
                showNoInternet()
            }
            mBinding.swipeRefreshLayout.isRefreshing = false
        }

        mBinding.logOut.setOnClickListener {
            logOutConfirmationDialog()
        }
        mBinding.deleteAccount.setOnClickListener {
            deleteAccountConfirmationDialog(email)
        }
        mBinding.instagramImageView.setOnClickListener {
            loadUrl("https://www.instagram.com/makes360_india/")
        }
        mBinding.facebookImageView.setOnClickListener {
            loadUrl("https://www.facebook.com/Makes360india/")
        }
        mBinding.youtubeImageView.setOnClickListener {
            loadUrl("https://www.youtube.com/@Makes360_innovations")
        }
        mBinding.linkedinImageView.setOnClickListener {
            loadUrl("https://www.linkedin.com/company/makes360/posts/")
        }
        mBinding.xImageView.setOnClickListener {
            loadUrl("https://x.com/makes360india")
        }
        mBinding.pinterestImageView.setOnClickListener {
            loadUrl("https://www.pinterest.com/makes360/makes360/")
        }
    }

    private fun loadUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun logOutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)

        // Create a custom title TextView
        val titleView = TextView(this)
        titleView.text = getString(R.string.log_out)
        titleView.setTextColor(Color.BLACK) // Set the title text color
        titleView.textSize = 20f
        titleView.setPadding(60, 60, 60, 10) // Add padding for better appearance
        titleView.gravity = Gravity.START
        titleView.setTypeface(null, Typeface.BOLD) // Set text to bold


        builder.setCustomTitle(titleView)
        builder.setMessage("Are you sure you want to log out of your account?")

        // Set up the buttons
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss() // Close the dialog
            logout() // Proceed with account deletion
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Close the dialog without doing anything
        }

        // Display the dialog
        val dialog = builder.create()
        dialog.show()

        // Apply rounded corners and background
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)

        // Customize the button appearance
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.material_flat_red))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.material_flat_green_dark))

        // Set title and message text color to black
        val textViewMessage = dialog.findViewById<TextView>(android.R.id.message)
        val textViewTitle = dialog.findViewById<TextView>(android.R.id.title)
        textViewMessage?.setTextColor(Color.BLACK)
        textViewTitle?.setTextColor(Color.BLACK)
    }

    private fun deleteAccountConfirmationDialog(email: String?) {
        val builder = AlertDialog.Builder(this)

        // Create a custom title TextView
        val titleView = TextView(this)
        titleView.text = getString(R.string.delete_account)
        titleView.setTextColor(Color.BLACK) // Set the title text color
        titleView.textSize = 20f
        titleView.setPadding(60, 60, 60, 10) // Add padding for better appearance
        titleView.gravity = Gravity.START
        titleView.setTypeface(null, Typeface.BOLD) // Set text to bold


        builder.setCustomTitle(titleView)
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.")

        // Set up the buttons
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss() // Close the dialog
            deleteAccount(email) // Proceed with account deletion
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Close the dialog without doing anything
        }

        // Display the dialog
        val dialog = builder.create()
        dialog.show()

        // Apply rounded corners and background
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)

        // Customize the button appearance
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.material_flat_red))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.material_flat_green_dark))

        // Set title and message text color to black
        val textViewMessage = dialog.findViewById<TextView>(android.R.id.message)
        val textViewTitle = dialog.findViewById<TextView>(android.R.id.title)
        textViewMessage?.setTextColor(Color.BLACK)
        textViewTitle?.setTextColor(Color.BLACK)
    }

    private fun deleteAccount(email: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://www.makes360.com/application/makes360/internship/delete-account.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                // Write email to request body
                val requestBody = "email=$email"
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()
                outputStream.close()

                // Read response
                val responseCode = connection.responseCode
                val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Parse response message
                    val message = JSONObject(responseMessage).optString("message", "Success")
                    val error = JSONObject(responseMessage).optString("error", "")

                    runOnUiThread {
                        hideLoader()
                        if (error.isNotEmpty()) {
                            showToast(error)
                        } else {
                            showToast(message)
                        }
                        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.clear() // Clear all stored data
                        editor.apply()

                        // Redirect to login screen
                        val intent = Intent(baseContext, InternLogin::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
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

    private fun showLoader() {
        mBinding.progressBar.visibility = View.VISIBLE
        mBinding.progressBar.playAnimation()
        mBinding.progressOverlay.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        mBinding.progressBar.cancelAnimation()
        mBinding.progressOverlay.visibility = View.GONE
    }

    private fun profileCategory(details: ProfileDetails) {
        val statusList = mutableListOf<String>()
        val profileImageView = findViewById<ImageView>(R.id.internImg)

        // Adding Image to the profileImageView

        if (details.profilePic.isEmpty() || details.profilePic == "null") {
            if (details.gender == "0")
                profileImageView.setImageResource(R.drawable.intern_girl)
            else
                profileImageView.setImageResource(R.drawable.intern_boy)
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

        // Adding elements to specific indexes
        statusList.add(0, "Successfully Applied")
        statusList.add(1, "Shortlisted - Upload Resume & Testimonials")
        statusList.add(2, "Rejected")
        statusList.add(3, "Call for Interview")
        statusList.add(4, "Got Offer Letter")
        statusList.add(5, "Doing Internship")
        statusList.add(6, "Internship Completed")


        val dob = formatDate(details.dob)
        val applyDate = formatDate(details.applyDate)
        val shortlistedDate = formatDate(details.shortlistedDate)
        val interviewDate = formatDate(details.callForInterviewDate)
        val joiningDate = formatDate(details.joiningDate)
        val rejectedDate = if (details.rejectedDate.isEmpty() || details.rejectedDate == "null")
            "Not Rejected"
        else
            formatDate(details.rejectedDate)

        val completeDate = if (details.completeDate.isEmpty() || details.completeDate == "null") {
            if (details.applicationStatus == 5) {
                "Internship Ongoing"
            } else {
                "Will be Updated"
            }
        } else
            formatDate(details.completeDate)


        val recyclerView: RecyclerView = findViewById(R.id.profile_recycler_view)

        fun String?.orPlaceholder() = if (this.isNullOrEmpty()) "Will be Updated" else this

        val categories = listOf(
            InternProfileCategory(
                "General",
                mapOf(
                    "Mother's Name:" to details.motherName.orPlaceholder(),
                    "Date of Birth:" to dob.orPlaceholder(),
                    "Gender:" to if (details.gender == "1") "Male" else "Female"
                ),
                icon = R.drawable.ic_general
            ),
            InternProfileCategory(
                "Contact Details",
                mapOf(
                    "Phone No:" to "${details.phoneCode.orPlaceholder()} ${details.phoneNumber.orPlaceholder()}",
                    "WhatsApp No:" to "${details.phoneCode.orPlaceholder()} ${details.whatsappNumber.orPlaceholder()}",
                    "Address:" to details.address.orPlaceholder()
                ),
                icon = R.drawable.ic_phone
            ),
            InternProfileCategory(
                "Academic Information",
                mapOf(
                    "College:" to details.college.orPlaceholder(),
                    "Department:" to details.department.orPlaceholder(),
                    "Roll Section:" to details.rollSection.orPlaceholder(),
                    "Passing Year:" to details.passingYear.orPlaceholder()
                ),
                icon = R.drawable.ic_academic
            ),
            InternProfileCategory(
                "Internship/Emp Details",
                buildMap {
                    put("Position:", details.internshipRole.orPlaceholder())
                    if (details.fullTime == "0") {
                        put("Duration:", "${details.internshipDuration.orPlaceholder()} Months")
                    }
                    if (details.currentStipend == "0") {
                        put("Stipend/Salary:", "Will be Updated")
                    } else {
                        put("Stipend/Salary:", "₹${details.currentStipend.orPlaceholder()}")
                    }
                    if (details.incrementRemarks.isEmpty() || details.incrementRemarks == "null") {
                        put("Incr. Remarks:", "Will be Updated")
                    } else {
                        put("Incr. Remarks:", details.incrementRemarks)
                    }
                },
                icon = R.drawable.ic_intern_bag,
                name = details.name,
                applicationStatus = details.applicationStatus,
                internID = details.certificateNumber,
                adminMobileNo = details.adminMobileNo
            ),
            InternProfileCategory(
                "Bank Details",
                mapOf(
                    "Bank Acc No:" to details.bankAccountNumber.orPlaceholder(),
                    "IFSC Code:" to details.ifscCode.orPlaceholder()
                ),
                icon = R.drawable.ic_bank
            ),
            InternProfileCategory(
                "Internship/Emp Status",
                buildMap {
                    put(
                        "Current Status:",
                        statusList.getOrNull(details.applicationStatus)?.orPlaceholder()
                            ?: "Unknown Status"
                    )
                    put(
                        "Certificate Status:",
                        if (details.certificateStatus == "1") "Certificate Allotted" else "Certificate Not Allotted"
                    )
                    put("Internship ID:", details.certificateNumber)
                    put("Apply Date:", applyDate.orPlaceholder())
                    if (details.applicationStatus == 3) {
                        put("Rejected Date:", rejectedDate)
                    }
                    put("Shortlisted Date:", shortlistedDate.orPlaceholder())
                    put("Interview Call:", interviewDate.orPlaceholder())
                    put("Joining Date:", joiningDate.orPlaceholder())
                    put("Completion Date:", completeDate.orPlaceholder())
                },
                icon = R.drawable.ic_application,
                applicationStatus = details.applicationStatus,
                videoTestimonialLink = details.videoTestimonialLink
            )
        )

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = InternProfileAdapter(this, categories)
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

    private fun logout() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Clear all stored data
        editor.apply()

        // Redirect to login screen
        val intent = Intent(this, InternLogin::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        Toast.makeText(this, "Log Out Successfully", Toast.LENGTH_SHORT).show()
        startActivity(intent)
    }

    private fun fetchInternProfile(email: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/internship/intern-profile-details.php")
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
            adminMobileNo = jsonResponse.optString("adminMob", "Unknown"),
            name = jsonResponse.optString("name", "Unknown"),
            email = jsonResponse.optString("email", "Unknown"),
            motherName = jsonResponse.optString("mother_name", "Unknown"),
            internshipDuration = jsonResponse.optString("intern_duration", "Unknown"),
            fullTime = jsonResponse.optString("fullTime", "Unknown"),
            phoneCode = jsonResponse.optString("phone_code", "Unknown"),
            phoneNumber = jsonResponse.optString("phone_number", "Unknown"),
            whatsappNumber = jsonResponse.optString("whatsapp_number", "Unknown"),
            profilePic = jsonResponse.optString("profile_pic", "Unknown"),
            address = jsonResponse.optString("address", "Unknown"),
            college = jsonResponse.optString("college", "Unknown"),
            department = jsonResponse.optString("department", "Unknown"),
            rollSection = jsonResponse.optString("roll_section", "Unknown"),
            passingYear = jsonResponse.optString("passing_year", "Unknown"),
            dob = jsonResponse.optString("dob", "Unknown"),
            gender = jsonResponse.optString("gender", "Unknown"),
            internshipRole = jsonResponse.optString("intern_role", "Unknown"),
            stipend = jsonResponse.optString("stipend", "Unknown"),
            baseStipend = jsonResponse.optString("base_stipend", "Unknown"),
            currentStipend = jsonResponse.optString("curr_stipend", "Unknown"),
            incrementRemarks = jsonResponse.optString("increment_remarks", "Unknown"),
            applicationStatus = jsonResponse.optInt("application_status", -1),
            certificateStatus = jsonResponse.optString("certificate_status", "Unknown"),
            certificateNumber = jsonResponse.optString("certificate_number", "Unknown"),
            applyDate = jsonResponse.optString("apply_date", "Unknown"),
            shortlistedDate = jsonResponse.optString("shortlisted_date", "Unknown"),
            callForInterviewDate = jsonResponse.optString("call_for_interview_date", "Unknown"),
            rejectedDate = jsonResponse.optString("rejectedDate", "Unknown"),
            joiningDate = jsonResponse.optString("join_date", "Unknown"),
            completeDate = jsonResponse.optString("complete_date", "Unknown"),
            videoTestimonialLink = jsonResponse.optString("video_testimonial_link", "Unknown"),
            bankAccountNumber = jsonResponse.optString("bank_ac_no", "Unknown"),
            ifscCode = jsonResponse.optString("ifsc", "Unknown")
        )
    }

    private fun updateProfileUI(details: ProfileDetails) {
        mBinding.internName.text = details.name
        mBinding.internEmail.text = intent.getStringExtra("EMAIL")
        mBinding.internRoleTxtView.text = details.internshipRole
        mBinding.internEmpID.text = "ID: " + details.certificateNumber
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class ProfileDetails(
        val adminMobileNo: String,
        val name: String,
        val email: String,
        val motherName: String,
        val internshipDuration: String,
        val fullTime: String,
        val phoneCode: String,
        val phoneNumber: String,
        val whatsappNumber: String,
        val profilePic: String,
        val address: String,
        val college: String,
        val department: String,
        val rollSection: String,
        val passingYear: String,
        val dob: String,
        val gender: String,
        val internshipRole: String,
        val stipend: String,
        val baseStipend: String,
        val currentStipend: String,
        val incrementRemarks: String,
        val applicationStatus: Int,
        val certificateStatus: String,
        val certificateNumber: String,
        val applyDate: String,
        val shortlistedDate: String,
        val callForInterviewDate: String,
        val rejectedDate: String,
        val joiningDate: String,
        val completeDate: String,
        val videoTestimonialLink: String,
        val bankAccountNumber: String,
        val ifscCode: String
    )
}
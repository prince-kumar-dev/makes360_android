package com.makes360.app.ui.client

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import com.makes360.app.BaseActivity
import com.makes360.app.databinding.ActivityClientProjectDetailsBinding
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

class ClientProjectDetails : BaseActivity() {

    private lateinit var mBinding: ActivityClientProjectDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }

    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityClientProjectDetailsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.clientProjectDetailsToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Get the email passed from the previous activity
        val projectId = intent.getStringExtra("PROJECT_ID")

        if (projectId != null) {
            showLoader()
            fetchProjectDetails(projectId.toInt())
        }

        mBinding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (projectId != null) {
                    fetchProjectDetails(projectId.toInt())
                }
            } else {
                showNoInternet()
            }
            mBinding.swipeRefreshLayout.isRefreshing = false
        }

        mBinding.backImageView.setOnClickListener {
            finish()
        }
    }

    private fun showLoader() {
        mBinding.progressOverlay.visibility = View.VISIBLE
        mBinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        mBinding.progressOverlay.visibility = View.GONE
        mBinding.progressBar.visibility = View.GONE
    }

    private fun updateProjectUI(project: ProjectDetails) {
        val currentStatusList = listOf(
            "In Progress 🛠️",
            "On Hold ⏸️",
            "Completed ✅",
            "Waiting Respond ⏳"
        )

        // Map statuses to progress values (e.g., "In Progress" = 50%, etc.)
        val progressValues = mapOf(
            0 to 50, // "In Progress"
            1 to 25, // "On Hold"
            2 to 100, // "Completed"
            3 to 10  // "Waiting Respond"
        )

        val renewalPeriodList =
            listOf("Monthly", "Quarterly", "Half Yearly", "Yearly", "One Time Payment")

        fun String?.orPlaceholder() = if (this.isNullOrEmpty()) "Will be Updated" else this


        // Animate and set Project Name
        mBinding.projectName.apply {
            text = project.projectName
        }

        // Update the ProgressBar and Status Text
        val progressBar = mBinding.projectStatusProgress
        val statusText = mBinding.projectStatusValue

        val statusIndex = project.currentStatus.toInt()
        val progress = progressValues[statusIndex] ?: 0
        val status = currentStatusList[statusIndex].orPlaceholder()

        // Animate the progress bar update
        ObjectAnimator.ofInt(progressBar, "progress", progress).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            start()
        }

        // Update status text
        statusText.text = status


        // Animate and set Start Date
        mBinding.projectStartDate.apply {
            text = formatDate(project.startDate)
        }

        // Animate and set Renewal Period
        mBinding.renewalPeriod.apply {
            text = renewalPeriodList[project.renewalPeriod.toInt()].orPlaceholder()
        }

        // Animate and set First Renewal Date
        mBinding.firstRenewalDate.apply {
            text = formatDate(project.firstRenewalDate).orPlaceholder()
        }

        // Animate and set Project Note
        mBinding.projectNote.apply {
            text = project.projectNote.orPlaceholder()
        }

        // Animate and set GST No
        mBinding.gstNo.apply {
            text = project.gstNo
        }

//        // Scale up-down animation on card container
//        val cardContainer = mBinding.cardContainer // Replace with your card container ID
//        cardContainer.setOnClickListener {
//            cardContainer.startAnimation(scaleUpDown)
//        }
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

    private fun fetchProjectDetails(projectId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/client/project-details.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                // Write custId to request body
                val requestBody = "project_id=$projectId"
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()
                outputStream.close()

                // Check response code
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val projectDetailsList = parseProjectResponse(response)

                    // Update UI on the main thread
                    runOnUiThread {
                        hideLoader()
                        if (projectDetailsList != null) {
                            updateProjectUI(projectDetailsList)
                        }
                    }
                } else {
                    runOnUiThread {
                        hideLoader()
                        showToast("Failed to fetch project details. Please try again.")
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

    private fun parseProjectResponse(response: String): ProjectDetails? {
        val jsonResponse = JSONObject(response)

        return if (jsonResponse.optBoolean("success", false)) {
            val projectJson = jsonResponse.optJSONObject("project")
            if (projectJson != null) {
                ProjectDetails(
                    projectId = projectJson.optInt("project_id"),
                    currentStatus = projectJson.optString("current_status", "Unknown"),
                    projectName = projectJson.optString("project_name", "Unknown"),
                    startDate = projectJson.optString("start_date", "Unknown"),
                    renewalPeriod = projectJson.optString("renewal_period", "Unknown"),
                    firstRenewalDate = projectJson.optString("first_renewal_date", "Unknown"),
                    projectNote = projectJson.optString("project_note", "Unknown"),
                    gstNo = projectJson.optString("gst_no", "Unknown")
                )
            } else {
                null
            }
        } else {
            runOnUiThread {
                showToast(jsonResponse.optString("error", "Unknown error occurred."))
            }
            null
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class ProjectDetails(
        val projectId: Int,
        val currentStatus: String,
        val projectName: String,
        val startDate: String,
        val renewalPeriod: String,
        val firstRenewalDate: String,
        val projectNote: String,
        val gstNo: String
    )

}
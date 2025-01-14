package com.makes360.app.ui.client

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.makes360.app.R
import com.makes360.app.adapters.client.ClientProjectDetailsAdapter
import com.makes360.app.databinding.ActivityClientProjectDetailsBinding
import com.makes360.app.models.client.ClientProjectDetailsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class ClientProjectDetails : AppCompatActivity() {

    private lateinit var mBinding: ActivityClientProjectDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize binding
        mBinding = ActivityClientProjectDetailsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.clientProjectDetailsToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Get the email passed from the previous activity
        val projectId = intent.getStringExtra("PROJECT_ID")
        showToast(projectId.toString())

        if (projectId != null) {
             showLoader()
             fetchProjectDetails(projectId.toInt())
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

        val renewalPeriodList = listOf("Monthly", "Quarterly", "Half Yearly", "Yearly", "One Time Payment")

        fun String?.orPlaceholder() = if (this.isNullOrEmpty()) "Will be Updated" else this

        val projectDetails = ClientProjectDetailsData(
            details = mapOf(
                    "Project Id:" to project.projectId.toString(),
                    "Project Name:" to project.projectName,
                    "Current Status:" to currentStatusList[project.currentStatus.toInt()].orPlaceholder(),
                    "Start Date:" to formatDate(project.startDate).orPlaceholder(),
                    "Renewal Period:" to renewalPeriodList[project.renewalPeriod.toInt()].orPlaceholder(),
                    "First Renewal Date:" to formatDate(project.firstRenewalDate).orPlaceholder(),
                    "Project Note:" to project.projectNote.orPlaceholder(),
                    "GST No:" to project.gstNo.orPlaceholder()
                )
        )

        mBinding.projectDetailsRV.layoutManager = LinearLayoutManager(this)
        mBinding.projectDetailsRV.adapter = ClientProjectDetailsAdapter(this, listOf(projectDetails))
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
                val url = URL("https://www.makes360.com/application/makes360/client/project-details.php")
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
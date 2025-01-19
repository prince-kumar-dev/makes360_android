package com.makes360.app.ui.client

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.makes360.app.BaseActivity
import com.makes360.app.databinding.ActivityClientProjectAssetsBinding
import com.makes360.app.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ClientProjectAssets : BaseActivity() {

    private lateinit var mBinding: ActivityClientProjectAssetsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }

    }

    private fun loadContent() {

        // Initialize binding
        mBinding = ActivityClientProjectAssetsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.clientProjectAssetsToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Get the email passed from the previous activity
        val projectId = intent.getStringExtra("PROJECT_ID")
        val projectName = intent.getStringExtra("PROJECT_NAME")

        if (projectId != null && projectName != null) {
            showLoader()
            fetchProjectAssetsDetails(projectId.toInt(), projectName)
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


    private fun fetchProjectAssetsDetails(projectId: Int, projectName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/client/project-assets.php")
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
                            mBinding.projectAssetsLayout.visibility = View.VISIBLE
                            updateProjectUI(projectDetailsList, projectName)
                        } else {
                            mBinding.projectAssetsLayout.visibility = View.GONE
                            mBinding.noProjectAssetsLayout.visibility = View.VISIBLE
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

    private fun updateProjectUI(projectAssetsDetails: ProjectAssets, projectName: String) {

        mBinding.tvProjectName.text = projectName
        mBinding.tvDomain.text = if (projectAssetsDetails.domain == 1) "Yes" else "No"
        mBinding.tvDomainRemarks.text = projectAssetsDetails.domainRemarks
        mBinding.tvHosting.text = if (projectAssetsDetails.hosting == 1) "Yes" else "No"
        mBinding.tvHostingRemarks.text = projectAssetsDetails.hostingRemarks
        mBinding.tvBusinessEmail.text = if (projectAssetsDetails.businessEmail == 1) "Yes" else "No"
        mBinding.tvBusinessEmailRemarks.text = projectAssetsDetails.businessEmailRemarks
        mBinding.tvDatabase.text = if (projectAssetsDetails.database == 1) "Yes" else "No"
        mBinding.tvDatabaseRemarks.text = projectAssetsDetails.databaseRemarks
        mBinding.tvSSLCertificate.text =
            if (projectAssetsDetails.sslCertificate == 1) "Yes" else "No"
        mBinding.tvSSLCertificateRemarks.text = projectAssetsDetails.sslCertificateRemarks
        mBinding.tvSourceCode.text = if (projectAssetsDetails.sourceCode == 1) "Yes" else "No"
        mBinding.tvSourceCodeRemarks.text = projectAssetsDetails.sourceCodeRemarks
        mBinding.tvSocialMediaAssets.text = projectAssetsDetails.socialMediaAssets
        mBinding.tvOtherAssets.text = projectAssetsDetails.otherAssets
    }

    private fun parseProjectResponse(response: String): ProjectAssets? {
        val jsonResponse = JSONObject(response)

        return if (jsonResponse.optBoolean("success", false)) {
            val projectJson = jsonResponse.optJSONObject("project")
            if (projectJson != null) {
                ProjectAssets(
                    domain = projectJson.optInt("domain"),
                    domainRemarks = projectJson.optString("domain_name_remarks", "Unknown"),
                    hosting = projectJson.optInt("hosting"),
                    hostingRemarks = projectJson.optString("hosting_remarks", "Unknown"),
                    businessEmail = projectJson.optInt("business_email"),
                    businessEmailRemarks = projectJson.optString("email_remarks", "Unknown"),
                    database = projectJson.optInt("db_database"),
                    databaseRemarks = projectJson.optString("database_remarks", "Unknown"),
                    sslCertificate = projectJson.optInt("ssl_certificate"),
                    sslCertificateRemarks = projectJson.optString("ssl_remarks", "Unknown"),
                    sourceCode = projectJson.optInt("source_code"),
                    sourceCodeRemarks = projectJson.optString("source_code_remarks", "Unknown"),
                    socialMediaAssets = projectJson.optString("social_media_assets", "Unknown"),
                    otherAssets = projectJson.optString("other_assets", "Unknown")
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

    data class ProjectAssets(
        val domain: Int,
        val domainRemarks: String,
        val hosting: Int,
        val hostingRemarks: String,
        val businessEmail: Int,
        val businessEmailRemarks: String,
        val database: Int,
        val databaseRemarks: String,
        val sslCertificate: Int,
        val sslCertificateRemarks: String,
        val sourceCode: Int,
        val sourceCodeRemarks: String,
        val socialMediaAssets: String,
        val otherAssets: String
    )

}
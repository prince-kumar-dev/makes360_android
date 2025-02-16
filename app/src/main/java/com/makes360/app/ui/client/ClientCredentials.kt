package com.makes360.app.ui.client

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.client.CredentialAdapter
import com.makes360.app.databinding.ActivityClientCredentialsBinding
import com.makes360.app.models.client.CredentialData
import com.makes360.app.util.NetworkUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ClientCredentials : BaseActivity() {

    private lateinit var mBinding: ActivityClientCredentialsBinding
    private lateinit var adapter: CredentialAdapter
    private lateinit var requestQueue: RequestQueue
    private val credentialsList = mutableListOf<CredentialData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityClientCredentialsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.credentialsToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        requestQueue = Volley.newRequestQueue(this)

        // Get the email passed from the previous activity
        val projectId = intent.getStringExtra("PROJECT_ID")
        val projectName = intent.getStringExtra("PROJECT_NAME")

        if (projectId != null) {
            showLoader()
            fetchCredentials(projectId.toInt())
        }

        mBinding.credentialsRV.layoutManager = LinearLayoutManager(this)
        adapter = CredentialAdapter(credentialsList, this)
        mBinding.credentialsRV.adapter = adapter


        mBinding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (projectId != null && projectName != null) {
                    fetchCredentials(projectId.toInt())
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

    private fun fetchCredentials(projectId: Int) {
        val url = "https://www.makes360.com/application/makes360/client/credentials.php"

        // Create a JSON object for the POST request body
        val requestBody = JSONObject()
        try {
            requestBody.put("project_id", projectId)
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to create request body", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a JsonObjectRequest for the POST request
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, requestBody,
            { response ->
                try {
                    // Check if the response contains success and handle accordingly
                    if (response.optBoolean("success", false)) {
                        val credentialsArray: JSONArray = response.getJSONArray("credentials_list")
                        credentialsList.clear() // Clear the list before adding new data

                        if (credentialsArray.length() == 0 || credentialsArray.isNull(0)) {
                            mBinding.noCredentialsLayout.visibility = View.VISIBLE
                            mBinding.credentialsRV.visibility = View.GONE
                        } else {
                            // Loop through the credentials array
                            for (i in credentialsArray.length() - 1 downTo 0) {

                                val item = credentialsArray.getJSONObject(i)
                                val name = item.getString("credentials_name")

                                val rawHtml = item.getString("credentials_value")
                                val value = HtmlCompat.fromHtml(
                                    rawHtml,
                                    HtmlCompat.FROM_HTML_MODE_LEGACY
                                ).toString().trim()

                                credentialsList.add(CredentialData(name, value))
                            }

                            // Set the adapter with fetched data
                            adapter = CredentialAdapter(credentialsList, this@ClientCredentials)
                            mBinding.credentialsRV.adapter = adapter
                            mBinding.noCredentialsLayout.visibility = View.GONE
                            mBinding.credentialsRV.visibility = View.VISIBLE
                        }

                        hideLoader() // Hide loader after data is successfully fetched
                    } else {
                        hideLoader()
                        mBinding.noCredentialsLayout.visibility = View.VISIBLE
                        mBinding.credentialsRV.visibility = View.GONE
                        val errorMessage = response.optString("error", "No credentials found.")
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    hideLoader()
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                hideLoader()
                error.printStackTrace()
                Toast.makeText(
                    this,
                    "Error fetching credentials: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        ) {
            // Set the content type to JSON
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        // Add the request to the RequestQueue
        showLoader() // Show loader before fetching data
        requestQueue.add(jsonObjectRequest)
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
}
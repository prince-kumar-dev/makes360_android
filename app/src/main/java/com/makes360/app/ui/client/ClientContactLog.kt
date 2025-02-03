package com.makes360.app.ui.client

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.client.ContactLogAdapter
import com.makes360.app.databinding.ActivityContactLogBinding
import com.makes360.app.models.client.ContactLogData
import com.makes360.app.util.NetworkUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClientContactLog : BaseActivity() {

    private lateinit var mBinding: ActivityContactLogBinding
    private lateinit var adapter: ContactLogAdapter
    private lateinit var requestQueue: RequestQueue
    private val contactLog = ArrayList<ContactLogData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityContactLogBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.contactLogToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        requestQueue = Volley.newRequestQueue(this)

        mBinding.contactLogRV.layoutManager = LinearLayoutManager(this)

        // Get the email passed from the previous activity
        val projectId = intent.getStringExtra("PROJECT_ID")
        val projectName = intent.getStringExtra("PROJECT_NAME")

        if (projectId != null && projectName != null) {
            showLoader()
            fetchProjectContactLog(projectId.toInt())
        }

        mBinding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (projectId != null && projectName != null) {
                    fetchProjectContactLog(projectId.toInt())
                }
            } else {
                showNoInternet()
            }
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

    private fun fetchProjectContactLog(projectId: Int) {
        val url = "https://www.makes360.com/application/makes360/client/contact-log.php"

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
                        val contactLogArray: JSONArray = response.getJSONArray("contact_log")
                        contactLog.clear() // Clear the list before adding new data

                        if (contactLogArray.length() == 0 || contactLogArray.isNull(0)) {
                            mBinding.noContactLogLayout.visibility = View.VISIBLE
                            mBinding.contactLogRV.visibility = View.GONE
                        } else {
                            // Loop through the contact log array
                            for (i in contactLogArray.length() - 1 downTo 0) {
                                val item = contactLogArray.getJSONObject(i)
                                val date = item.getString("date")
                                val log = item.getString("log")

                                contactLog.add(ContactLogData(date, log))
                            }

                            // Set the adapter with fetched data
                            adapter = ContactLogAdapter(contactLog)
                            mBinding.contactLogRV.adapter = adapter
                        }

                        hideLoader() // Hide loader after data is successfully fetched
                    } else {
                        hideLoader()
                        val errorMessage = response.optString("error", "No contact log found.")
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
                    "Error fetching contact log: ${error.message}",
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.filter_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_apply_filter -> {
                showDatePicker()
                return true
            }

            R.id.menu_clear_filter -> {
                clearFilter()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    dayOfMonth
                )
                filterContactLogByDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun filterContactLogByDate(selectedDate: String) {
        try {
            // Parse the selected date (YYYY-MM-DD) and reformat it to DD-MMM-YYYY
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Filter announcements based on the reformatted date
            val filteredList = contactLog.filter {
                it.date == inputDateFormat.format(
                    inputDateFormat.parse(selectedDate)!!
                )
            }
            if (filteredList.isNotEmpty()) {
                adapter.setContactLogs(filteredList)
                mBinding.contactLogRV.scrollToPosition(0)
            } else {
                Toast.makeText(this, "No announcements found for this date", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error parsing date", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFilter() {
        // Reset the RecyclerView with the original list
        adapter.setContactLogs(contactLog)
        mBinding.contactLogRV.scrollToPosition(0)
        Toast.makeText(this, "Filter cleared", Toast.LENGTH_SHORT).show()
    }
}
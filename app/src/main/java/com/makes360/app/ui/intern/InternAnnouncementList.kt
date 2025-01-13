package com.makes360.app.ui.intern

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.makes360.app.BaseActivity
import com.makes360.app.adapters.InternAnnouncementListAdapter
import com.makes360.app.models.InternAnnouncementListRV
import com.makes360.app.R
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InternAnnouncementList : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InternAnnouncementListAdapter
    private lateinit var requestQueue: RequestQueue
    private lateinit var progressOverlay: View
    private lateinit var progressBar: ProgressBar
    private val announcements = ArrayList<InternAnnouncementListRV>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intern_announcement_list)

        setSupportActionBar(findViewById(R.id.announcementListToolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)

        recyclerView = findViewById(R.id.announcementListRV)
        recyclerView.layoutManager = LinearLayoutManager(this)
        progressBar = findViewById(R.id.progressBar)
        progressOverlay = findViewById(R.id.progressOverlay)

        requestQueue = Volley.newRequestQueue(this)

        fetchAnnouncements()
        setUpBackButton()
    }

    private fun showLoader() {
        progressOverlay.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressOverlay.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun setUpBackButton() {
        findViewById<ImageView>(R.id.backImageView).setOnClickListener {
            finish()
        }
    }

    private fun fetchAnnouncements() {
//        if (!checkInternetConnection()) {
//            hideLoader()
//            Toast.makeText(this, "No internet connection. Please check your connection.", Toast.LENGTH_LONG).show()
//            return
//        }

        val url = "https://www.makes360.com/application/makes360/internship/announcement.php"

        showLoader() // Show loader before fetching data

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {

                    // Check if the response contains announcements
                    if (response.has("error")) {
                        hideLoader()
                        Toast.makeText(this, response.getString("error"), Toast.LENGTH_SHORT).show()
                        return@JsonObjectRequest
                    }

                    val announcementArray: JSONArray = response.getJSONArray("announcements")

                    // Reverse Loop
                    for (i in announcementArray.length() - 1 downTo 0) {
                        val item = announcementArray.getJSONObject(i)
                        val date = item.getString("date")
                        val message = item.getString("message")

                        announcements.add(InternAnnouncementListRV(date, message))
                    }

                    // Set the adapter with fetched data
                    adapter = InternAnnouncementListAdapter(announcements)
                    recyclerView.adapter = adapter

                    hideLoader() // Hide loader after data is successfully fetched

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
                    "Error fetching announcements: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        // Add the request to the RequestQueue
        requestQueue.add(jsonObjectRequest)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.announcement_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_calendar -> {
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
                val selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                filterAnnouncementsByDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun filterAnnouncementsByDate(selectedDate: String) {
        try {
            // Parse the selected date (YYYY-MM-DD) and reformat it to DD-MMM-YYYY
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Filter announcements based on the reformatted date
            val filteredList = announcements.filter { it.date == inputDateFormat.format(inputDateFormat.parse(selectedDate)!!) }
            if (filteredList.isNotEmpty()) {
                adapter.setAnnouncements(filteredList)
                recyclerView.scrollToPosition(0)
            } else {
                Toast.makeText(this, "No announcements found for this date", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error parsing date", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFilter() {
        // Reset the RecyclerView with the original list
        adapter.setAnnouncements(announcements)
        recyclerView.scrollToPosition(0)
        Toast.makeText(this, "Filter cleared", Toast.LENGTH_SHORT).show()
    }
}
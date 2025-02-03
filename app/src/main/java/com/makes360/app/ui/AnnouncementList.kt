package com.makes360.app.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.makes360.app.BaseActivity
import com.makes360.app.adapters.AnnouncementListAdapter
import com.makes360.app.models.AnnouncementListData
import com.makes360.app.R
import com.makes360.app.util.NetworkUtils
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnnouncementList : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnnouncementListAdapter
    private lateinit var requestQueue: RequestQueue
    private lateinit var progressOverlay: View
    private lateinit var progressBar: LottieAnimationView
    private val announcements = ArrayList<AnnouncementListData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        setContentView(R.layout.activity_announcement_list)

        setSupportActionBar(findViewById(R.id.announcementListToolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val checkCandidate = intent.getStringExtra("CHECK_CANDIDATE")

        recyclerView = findViewById(R.id.announcementListRV)
        recyclerView.layoutManager = LinearLayoutManager(this)
        progressBar = findViewById(R.id.progressBar)
        progressOverlay = findViewById(R.id.progressOverlay)

        requestQueue = Volley.newRequestQueue(this)

        fetchAnnouncements(checkCandidate)
        setUpBackButton()
    }

    private fun showLoader() {
        progressBar.visibility = View.VISIBLE
        progressBar.playAnimation()
        progressOverlay.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressBar.cancelAnimation()
        progressOverlay.visibility = View.GONE
    }

    private fun setUpBackButton() {
        findViewById<ImageView>(R.id.backImageView).setOnClickListener {
            finish()
        }
    }

    private fun fetchAnnouncements(checkCandidate: String?) {

        val url = if (checkCandidate.equals("Client")) {
            "https://www.makes360.com/application/makes360/client/announcement.php"
        } else if (checkCandidate.equals("Intern")){
            "https://www.makes360.com/application/makes360/internship/announcement.php"
        } else {
            "https://www.makes360.com/application/makes360/trainee/announcement.php"
        }

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

                        announcements.add(AnnouncementListData(date, message))
                    }

                    // Set the adapter with fetched data
                    adapter = AnnouncementListAdapter(announcements)
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
            val filteredList = announcements.filter {
                it.date == inputDateFormat.format(
                    inputDateFormat.parse(selectedDate)!!
                )
            }
            if (filteredList.isNotEmpty()) {
                adapter.setAnnouncements(filteredList)
                recyclerView.scrollToPosition(0)
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
        adapter.setAnnouncements(announcements)
        recyclerView.scrollToPosition(0)
        Toast.makeText(this, "Filter cleared", Toast.LENGTH_SHORT).show()
    }
}
package com.makes360.app.ui.intern

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.InternTaskAssignAdapter
import com.makes360.app.databinding.ActivityInternTaskAssignBinding
import com.makes360.app.models.InternTaskAssignData
import com.makes360.app.util.NetworkUtils
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InternTaskAssign : BaseActivity() {

    private lateinit var mBinding: ActivityInternTaskAssignBinding
    private lateinit var adapter: InternTaskAssignAdapter
    private lateinit var requestQueue: RequestQueue
    private var taskList = mutableListOf<InternTaskAssignData>()
    private var filteredTaskList = mutableListOf<InternTaskAssignData>()
    private var selectedStatuses = mutableSetOf<String>()
    private var selectedPriorities = mutableSetOf<String>()
    private var startDate: String? = null
    private var endDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {

        // Initialize binding
        mBinding = ActivityInternTaskAssignBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.internTaskAssignToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        requestQueue = Volley.newRequestQueue(this)

        val email = intent.getStringExtra("EMAIL")

        email?.let {
            showLoader()
            fetchTaskDetails(it)
        }

        mBinding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                email?.let {
                    resetAllFiltersUI() // A function to reset UI filters (explained below)
                    showLoader()
                    fetchTaskDetails(it)
                }
            } else {
                showNoInternet()
            }
            mBinding.swipeRefreshLayout.isRefreshing = false
        }

        setUpBackButton()
        setupFilterListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.filter_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_apply_filter -> {
                showDateRangeDialog()
                return true
            }

            R.id.menu_clear_filter -> {
                resetAllFiltersUI()
                Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpBackButton() {
        findViewById<ImageView>(R.id.backImageView).setOnClickListener {
            finish()
        }
    }

    private fun resetAllFiltersUI() {
        // Clear UI filter selections
        selectedStatuses.clear()
        selectedPriorities.clear()
        startDate = null
        endDate = null

        // Restore original task list
        adapter.updateTaskList(taskList)

        // Hide the "No task found" layout when refreshing
        mBinding.noTaskFoundLayout.visibility = View.GONE

        val allFilters = listOf(
            Pair(mBinding.filterNotStarted, mBinding.filterNotStartedTextView),
            Pair(mBinding.filterInProgress, mBinding.filterInProgressTextView),
            Pair(mBinding.filterTesting, mBinding.filterTestingTextView),
            Pair(mBinding.filterFeedback, mBinding.filterFeedbackTextView),
            Pair(mBinding.filterCompleted, mBinding.filterCompletedTextView),
            Pair(mBinding.lowStatusCardView, mBinding.lowStatusTextView),
            Pair(mBinding.mediumStatusCardView, mBinding.mediumStatusTextView),
            Pair(mBinding.highStatusCardView, mBinding.highStatusTextView),
            Pair(mBinding.urgentStatusCardView, mBinding.urgentStatusTextView)
        )

        allFilters.forEach { (card, textView) ->
            resetFilter(card, textView)
        }
    }

    private fun setupFilterListeners() {
        // Status Filters
        val statusFilters = listOf(
            Triple(mBinding.filterNotStarted, mBinding.filterNotStartedTextView, "Not Started"),
            Triple(mBinding.filterInProgress, mBinding.filterInProgressTextView, "In Progress"),
            Triple(mBinding.filterTesting, mBinding.filterTestingTextView, "Testing"),
            Triple(mBinding.filterFeedback, mBinding.filterFeedbackTextView, "Awaiting Feedback"),
            Triple(mBinding.filterCompleted, mBinding.filterCompletedTextView, "Completed")
        )

        statusFilters.forEach { (card, textView, status) ->
            card.setOnClickListener {
                if (selectedStatuses.contains(status)) {
                    selectedStatuses.remove(status)
                    resetFilter(card, textView)
                } else {
                    selectedStatuses.add(status)
                    applyFilter(card, textView)
                }
                applyFilters()
            }
        }

        // Priority Filters
        val priorityFilters = listOf(
            Triple(mBinding.lowStatusCardView, mBinding.lowStatusTextView, "Low"),
            Triple(mBinding.mediumStatusCardView, mBinding.mediumStatusTextView, "Medium"),
            Triple(mBinding.highStatusCardView, mBinding.highStatusTextView, "High"),
            Triple(mBinding.urgentStatusCardView, mBinding.urgentStatusTextView, "Urgent")
        )

        priorityFilters.forEach { (card, textView, priority) ->
            card.setOnClickListener {
                if (selectedPriorities.contains(priority)) {
                    selectedPriorities.remove(priority)
                    resetFilter(card, textView)
                } else {
                    selectedPriorities.add(priority)
                    applyFilter(card, textView)
                }
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        filteredTaskList.clear()

        // Apply filtering logic
        filteredTaskList.addAll(taskList.filter { task ->
            val statusMatch = selectedStatuses.isEmpty() || selectedStatuses.contains(task.status)
            val priorityMatch =
                selectedPriorities.isEmpty() || selectedPriorities.contains(task.priority)
            val dateMatch = isWithinDateRange(task.startDate)

            statusMatch && priorityMatch && dateMatch
        })

        // If no filters are applied, show full task list
        if (selectedStatuses.isEmpty() && selectedPriorities.isEmpty() && startDate == null && endDate == null) {
            adapter.updateTaskList(taskList)
            mBinding.noTaskFoundLayout.visibility = View.GONE  // Hide "No tasks found" layout
        } else {
            if (filteredTaskList.isEmpty()) {
                mBinding.noTaskFoundLayout.visibility = View.VISIBLE // Show "No tasks found" layout
            } else {
                mBinding.noTaskFoundLayout.visibility = View.GONE  // Hide "No tasks found" layout
            }
            adapter.updateTaskList(filteredTaskList)
        }
    }

    private fun isWithinDateRange(taskStartDate: String): Boolean {
        if (startDate == null || endDate == null) return true // No filter applied

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val filterFormat = SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault())

        return try {
            val taskDate = inputFormat.parse(taskStartDate)!!
            val start = filterFormat.parse(startDate!!)!!
            val end = filterFormat.parse(endDate!!)!!

            taskDate in start..end
        } catch (e: Exception) {
            false
        }
    }


    // Helper function to highlight selected filter
    private fun applyFilter(card: MaterialCardView, textView: MaterialTextView) {
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.black))
        textView.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    // Helper function to reset unselected filter
    private fun resetFilter(card: MaterialCardView, textView: MaterialTextView) {
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
        textView.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
    }

    private fun showDateRangeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val startDateBtn = dialogView.findViewById<Button>(R.id.btnStartDate)
        val endDateBtn = dialogView.findViewById<Button>(R.id.btnEndDate)
        val applyButton = dialogView.findViewById<Button>(R.id.btnApplyFilters)

        startDateBtn.setOnClickListener { showDatePicker(startDateBtn) }
        endDateBtn.setOnClickListener { showDatePicker(endDateBtn) }

        applyButton.setOnClickListener {
            startDate = startDateBtn.text.toString()
            endDate = endDateBtn.text.toString()

            if (startDate!!.isNotEmpty() && endDate!!.isNotEmpty()) {
                applyFilters()
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun showDatePicker(dateBtn: Button) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault())
                    .format(Calendar.getInstance().apply { set(year, month, dayOfMonth) }.time)
                dateBtn.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
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

    private fun fetchTaskDetails(email: String) {
        val statusList =
            listOf("Not Started", "In Progress", "Testing", "Awaiting Feedback", "Completed")
        val priorityList = listOf("Low", "Medium", "High", "Urgent")

        val url =
            "https://www.makes360.com/application/makes360/internship/tasks-assign.php"

        // Create a JSON object for the POST request body
        val requestBody = JSONObject()
        try {
            requestBody.put("email", email)
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
                    // Check if the response contains tasks
                    val tasksArray = response.optJSONArray("tasks")

                    if (tasksArray != null && tasksArray.length() > 0) {
                        taskList.clear() // Clear the list before adding new data

                        // Loop through the tasks array
                        for (i in 0 until tasksArray.length()) {
                            val taskObj = tasksArray.getJSONObject(i)
                            val rawHtml = taskObj.optString("description")

                            val task = InternTaskAssignData(
                                name = taskObj.optString("name"),
                                description = HtmlCompat.fromHtml(
                                    rawHtml,
                                    HtmlCompat.FROM_HTML_MODE_LEGACY
                                ).toString().trim(),
                                priority = priorityList[taskObj.optInt("priority") - 1],
                                dueDate = if (taskObj.optString("duedate") == "null") {
                                    "Not Given"
                                } else {
                                    taskObj.optString("duedate")
                                },
                                startDate = taskObj.optString("startdate"),
                                finishDate = taskObj.optString("datefinished"),
                                status = statusList[taskObj.optInt("status") - 1]
                            )
                            taskList.add(task)
                        }

                        // Set up RecyclerView with fetched data
                        adapter = InternTaskAssignAdapter(taskList)
                        mBinding.taskRecyclerView.adapter = adapter
                        mBinding.taskRecyclerView.layoutManager = LinearLayoutManager(this)
                    } else {
                        // Handle the case where no tasks are found
                        mBinding.noTaskFoundLayout.visibility = View.VISIBLE
                        mBinding.taskRecyclerView.visibility = View.GONE
                        Toast.makeText(this, "No tasks found", Toast.LENGTH_SHORT).show()
                    }

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
                    "Error fetching tasks: ${error.message}",
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

        requestQueue.add(jsonObjectRequest)
    }
}
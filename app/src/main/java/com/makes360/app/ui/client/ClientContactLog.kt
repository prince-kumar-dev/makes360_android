package com.makes360.app.ui.client

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
            mBinding.swipeRefreshLayout.isRefreshing = false
        }

        mBinding.backImageView.setOnClickListener {
            finish()
        }

        mBinding.fabExport.setOnClickListener {
            exportToPDF()
        }

        setUpSearchView()

    }


    private fun setUpSearchView() {
        // Get references
        val searchCardView = mBinding.searchCardView  // Ensure this is available in your binding
        val searchView = mBinding.searchView

        // Ensure the search view is focusable when tapping the card view
        searchCardView.setOnClickListener {
            // Request focus and show keyboard
            searchView.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }

        // Change text and hint color
        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(Color.BLACK)
        searchText.setHintTextColor(Color.BLACK)

        // Change search icon color
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

        // Change clear (cross) icon color
        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

        // Set up the query text listener to filter adapter content
        mBinding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Optional: Hide keyboard or perform search action
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Pass the query to the adapter
                // Ensure your adapter instance is accessible here (e.g., adapter)
                adapter.filter(newText.orEmpty())
                return true
            }
        })
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
                        mBinding.noContactLogLayout.visibility = View.VISIBLE
                        mBinding.contactLogRV.visibility = View.GONE
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportToPDF() {
        try {
            // A4 dimensions (approximate)
            val pageWidth = 595
            val pageHeight = 842
            val margin = 50f
            val rightMargin = 50f
            val textStartY = 150f
            val lineSpacing = 25f
            val maxTextWidth = pageWidth - margin - rightMargin

            // Create PDF document
            val pdfDocument = PdfDocument()
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
            }

            // Load letterhead image
            val letterHead = BitmapFactory.decodeResource(resources, R.drawable.makes360_letter_head_bg)
            val resizedLetterHead = Bitmap.createScaledBitmap(letterHead, pageWidth, 120, true)

            var y = textStartY
            var pageNumber = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas

            // Draw letterhead at the top
            canvas.drawBitmap(resizedLetterHead, 0f, 0f, null)

            // Process each contact log
            contactLog.forEach { log ->
                val cleanText = HtmlCompat.fromHtml(log.message, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                val textLines = splitTextIntoLines(cleanText, paint, maxTextWidth)

                // Check if adding this log will exceed the page height
                if (y + (1 + textLines.size) * lineSpacing > pageHeight) {
                    pdfDocument.finishPage(page)  // Finish current page

                    // Start a new page
                    pageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    canvas.drawBitmap(resizedLetterHead, 0f, 0f, null)
                    y = textStartY
                }

                // Draw log date
                canvas.drawText("${log.date}:", margin, y, paint)
                y += lineSpacing

                // Draw log message lines
                for (line in textLines) {
                    canvas.drawText(line, margin, y, paint)
                    y += lineSpacing
                }
            }

            // Ensure the last page is finished
            pdfDocument.finishPage(page)

            // Generate unique filename
            val fileName = "makes360_contact_log_${System.currentTimeMillis()}.pdf"

            // Save PDF to Downloads using MediaStore
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { outStream ->
                    pdfDocument.writeTo(outStream)
                }
                pdfDocument.close()
                Toast.makeText(this, "PDF saved to Downloads!", Toast.LENGTH_LONG).show()
            } else {
                pdfDocument.close()
                Toast.makeText(this, "Failed to create PDF file", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Splits text into multiple lines based on maxWidth.
     */
    private fun splitTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

}
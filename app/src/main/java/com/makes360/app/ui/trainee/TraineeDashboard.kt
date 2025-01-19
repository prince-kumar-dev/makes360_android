package com.makes360.app.ui.trainee

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.trainee.TraineeDetailsAdapter
import com.makes360.app.models.trainee.TraineeDetailsData
import com.makes360.app.ui.AnnouncementList
import com.makes360.app.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TraineeDashboard : BaseActivity() {

    private lateinit var progressOverlay: View
    private lateinit var progressBar: ProgressBar
    private var traineeDetailsList = mutableListOf<TraineeDetailsData>()
    private lateinit var traineeDetailsAdapter: TraineeDetailsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        setContentView(R.layout.activity_trainee_dashboard)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.traineeToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        progressOverlay = findViewById(R.id.progressOverlay)
        progressBar = findViewById(R.id.progressBar)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")

        if (email != null) {
            showLoader()
            fetchTraineeDetails(email)
        } else {
            showToast("Invalid email! Please try again.")
        }
        setUpViews()
    }

    private fun setUpViews() {
        imageSlider()
        announcementList()
    }

    private fun imageSlider() {
        // Initialize the ImageSlider
        val imageSlider = findViewById<ImageSlider>(R.id.imageSlider)
        val imageList = ArrayList<SlideModel>()

        for (i in 1 until 4) {
            // Append a timestamp to force reload
            val imageUrl =
                "https://www.makes360.com/application/makes360/trainee/slider/image$i.png?timestamp=${System.currentTimeMillis()}"
            imageList.add(SlideModel(imageUrl))
        }

        imageSlider.setImageList(imageList, ScaleTypes.FIT)
    }

    private fun announcementWebView(email: String) {
        val cardView = findViewById<CardView>(R.id.markAsReadCardView)
        val cardViewText = findViewById<TextView>(R.id.markAsReadTxtView)
        val announcementWebView = findViewById<WebView>(R.id.announcementWebView)

        announcementWebView.setOnLongClickListener {
            // Do nothing on long press
            true
        }

        // Configure WebView settings
        val webSettings: WebSettings = announcementWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.textZoom = 220

        // Load content
        announcementWebView.webViewClient = WebViewClient()


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/trainee/announcement.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.optBoolean("success", false)

                    if (success) {
                        val announcements = jsonResponse.getJSONArray("announcements")
                        val currentDate = getCurrentDate()
                        val currentAnnouncement = (0 until announcements.length())
                            .map { announcements.getJSONObject(it) }
                            .find { it.getString("date") == currentDate }

                        // Fallback to the last non-empty message if no announcement is found for the current date
                        val announcementToShow =
                            currentAnnouncement ?: (announcements.length() - 1 downTo 0)
                                .map { announcements.getJSONObject(it) }
                                .find { it.optString("message").isNotEmpty() }

                        withContext(Dispatchers.Main) {
                            hideLoader()
                            if (currentAnnouncement != null) {
                                val id = currentAnnouncement.getString("id")
                                val message = currentAnnouncement.getString("message")
                                val readBy = currentAnnouncement.getString("read_by")
                                val isRead = readBy.split(",").contains(email)

                                announcementWebView.loadDataWithBaseURL(
                                    null,
                                    message,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )

                                if (isRead) {
                                    cardView.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            this@TraineeDashboard,
                                            R.color.material_core_light_green
                                        )
                                    )
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@TraineeDashboard,
                                            R.color.primary_text
                                        )
                                    )
                                    cardViewText.text = "Great! You Read It"
                                    cardView.isClickable = false
                                } else {
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@TraineeDashboard,
                                            R.color.white
                                        )
                                    )
                                    cardView.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            this@TraineeDashboard,
                                            R.color.colorPrimary
                                        )
                                    )
                                    cardView.isClickable = true
                                    cardView.setOnClickListener {
                                        markAsRead(email, id, cardView, cardViewText)
                                    }
                                }
                            } else {
                                val id = announcementToShow?.getString("id")
                                val message = announcementToShow?.getString("message")
                                val readBy = announcementToShow?.getString("read_by")
                                val isRead = readBy?.split(",")?.contains(email)

                                if (message != null) {
                                    announcementWebView.loadDataWithBaseURL(
                                        null,
                                        message,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }

                                if (isRead == true) {
                                    cardView.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            this@TraineeDashboard,
                                            R.color.material_core_light_green
                                        )
                                    )
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@TraineeDashboard,
                                            R.color.primary_text
                                        )
                                    )
                                    cardViewText.text = "Great! You Read It"
                                    cardView.isClickable = false
                                } else {
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@TraineeDashboard,
                                            R.color.white
                                        )
                                    )
                                    cardView.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            this@TraineeDashboard,
                                            R.color.colorPrimary
                                        )
                                    )
                                    cardView.isClickable = true
                                    cardView.setOnClickListener {
                                        if (id != null) {
                                            markAsRead(
                                                email,
                                                id,
                                                cardView,
                                                cardViewText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            hideLoader()
                            cardView.visibility = View.GONE
                            announcementWebView.loadData(
                                "<h1>No announcement for today.</h1>",
                                "text/html",
                                "UTF-8"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    hideLoader()
                    showToast("Error: ${e.message}")
                    announcementWebView.loadData(
                        "<h1>Error fetching announcements: ${e.message}</h1>",
                        "text/html",
                        "UTF-8"
                    )
                }
            }
        }
    }

    private fun markAsRead(
        email: String,
        announcementId: String,
        cardView: CardView,
        cardViewText: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/trainee/mark-as-read.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                val requestBody =
                    "email=$email&announcementId=$announcementId"
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()
                outputStream.close()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.optBoolean("success", false)

                    withContext(Dispatchers.Main) {
                        if (success) {
                            cardView.setCardBackgroundColor(
                                ContextCompat.getColor(
                                    this@TraineeDashboard,
                                    R.color.material_core_light_green
                                )
                            )
                            cardViewText.setTextColor(
                                ContextCompat.getColor(
                                    this@TraineeDashboard,
                                    R.color.primary_text
                                )
                            )
                            cardView.isClickable = false
                            cardViewText.text = "Great! You Read It"
                            showToast("Marked as read successfully!")
                        } else {
                            showToast(
                                "Failed to mark as read: ${
                                    jsonResponse.optString(
                                        "error",
                                        "Unknown error"
                                    )
                                }"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Error marking as read: ${e.message}")
                }
            }
        }
    }

    // Helper function to get the current date in "yyyy-MM-dd" format
    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }


    private fun announcementList() {
        val cardView =
            findViewById<CardView>(R.id.previousAnnouncementCardView)
        cardView.setOnClickListener {
            val intent = Intent(this, AnnouncementList::class.java)
            intent.putExtra("CHECK_CANDIDATE", "Trainee")
            startActivity(intent)
        }
    }

    private fun fetchTraineeDetails(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/trainee/trainee-profile-details.php")
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
                    val parsedData = parseResponse(response)

                    // Update UI on the main thread
                    runOnUiThread {
                        hideLoader()
                        traineeDetailsRecyclerView(parsedData)
                        announcementWebView(parsedData.status)
                    }
                } else {
                    runOnUiThread {
                        hideLoader()
                        showToast("Failed to fetch details. Please try again.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    hideLoader()
                    showToast("Error occurred: ${e.message}")
                }
            }
        }
    }

    private fun traineeDetailsRecyclerView(details: TraineeDetails) {
        val traineeDetailsRecyclerView =
            findViewById<RecyclerView>(R.id.detailsRecyclerView)

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Profile Details",
                icon = when (details.gender) {
                    "null" -> R.drawable.ic_question_mark
                    "0" -> R.drawable.intern_girl
                    else -> R.drawable.intern_boy
                },
                email = details.email
            )
        )

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Offer Letter",
                icon = R.drawable.ic_offer_letter
            )
        )

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Schedule",
                icon = R.drawable.ic_calendar
            )
        )

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Profile Update",
                icon = R.drawable.ic_update
            )
        )

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Leaderboard",
                icon = R.drawable.ic_leaderboard
            )
        )

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Fee Info",
                icon = R.drawable.ic_info
            )
        )

        traineeDetailsAdapter = TraineeDetailsAdapter(this, traineeDetailsList)
        traineeDetailsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        traineeDetailsRecyclerView.adapter = traineeDetailsAdapter

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.traineeToolbar)
        val titleToolbarTxt = toolbar.findViewById<TextView>(R.id.titleToolbarTxt)
        val name = details.name.substringBefore(" ") // Extract name up to the first space
        titleToolbarTxt.text = "Hi, $name!"
    }

    private fun parseResponse(response: String): TraineeDetails {
        val jsonResponse = JSONObject(response)
        return TraineeDetails(
            name = jsonResponse.optString("name", "Unknown"),
            points = jsonResponse.optString("points", "Unknown"),
            pointsRemarks = jsonResponse.optString("points_remarks", "Unknown"),
            offerLetter = jsonResponse.optString("offer_letter", "Unknown"),
            motherName = jsonResponse.optString("mother_name", "Unknown"),
            email = jsonResponse.optString("email", "Unknown"),
            phone = jsonResponse.optString("phone", "Unknown"),
            college = jsonResponse.optString("college", "Unknown"),
            department = jsonResponse.optString("department", "Unknown"),
            rollNo = jsonResponse.optString("roll_no", "Unknown"),
            dob = jsonResponse.optString("dob", "Unknown"),
            gender = jsonResponse.optString("gender", "Unknown"),
            address = jsonResponse.optString("address", "Unknown"),
            status = jsonResponse.optString("status", "Unknown"),
            txnId = jsonResponse.optString("txnId", "Unknown"),
            createdAt = jsonResponse.optString("created_at", "Unknown"),
            updated = jsonResponse.optString("updated", "Unknown")
        )
    }


    private fun showLoader() {
        progressOverlay.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressOverlay.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class TraineeDetails(
        val name: String,
        val points: String,
        val pointsRemarks: String,
        val offerLetter: String,
        val motherName: String,
        val email: String,
        val phone: String,
        val college: String,
        val department: String,
        val rollNo: String,
        val dob: String,
        val gender: String,
        val address: String,
        val status: String,
        val txnId: String,
        val createdAt: String,
        val updated: String
    )
}
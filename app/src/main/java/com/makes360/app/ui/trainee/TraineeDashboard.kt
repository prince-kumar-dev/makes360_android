package com.makes360.app.ui.trainee

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.trainee.TraineeDetailsAdapter
import com.makes360.app.databinding.ActivityTraineeDashboardBinding
import com.makes360.app.models.trainee.TraineeDetailsData
import com.makes360.app.ui.AnnouncementList
import com.makes360.app.ui.client.ClientDashboard
import com.makes360.app.ui.client.ClientLogin
import com.makes360.app.ui.intern.InternDashboard
import com.makes360.app.ui.intern.InternLogin
import com.makes360.app.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class TraineeDashboard : BaseActivity() {

    private lateinit var progressOverlay: View
    private lateinit var progressBar: LottieAnimationView
    private var traineeDetailsList = mutableListOf<TraineeDetailsData>()
    private lateinit var traineeDetailsAdapter: TraineeDetailsAdapter
    private lateinit var mBinding: ActivityTraineeDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityTraineeDashboardBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.traineeToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Set up ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this,
            mBinding.drawerLayout,
            mBinding.traineeToolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )

        mBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Add hamburger menu icon
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeAsUpIndicator(R.drawable.ic_menu) // Use your hamburger icon

        mBinding.navigationView.itemIconTintList = null

        val navigationView = mBinding.navigationView

        // List of menu items
        val menuItems = listOf(
            Pair(R.drawable.ic_admin, "Admin Login"),
            Pair(R.drawable.ic_man_client, "Client Login"),
            Pair(R.drawable.ic_nav_intern, "Intern/Emp Login"),
            Pair(R.drawable.ic_intern, "Trainee Login"),
            Pair(R.drawable.ic_nav_rate_us, "Rate Us"),
            Pair(R.drawable.ic_nav_share, "Share")
        )

        // Add each custom item to the NavigationView
        val menuParent =
            navigationView.getHeaderView(0).findViewById<LinearLayout>(R.id.menu_container)

        menuItems.forEach { (iconRes, title) ->
            val customView = layoutInflater.inflate(R.layout.item_nav_menu, menuParent, false)

            val iconView = customView.findViewById<ImageView>(R.id.icon)
            val titleView = customView.findViewById<TextView>(R.id.title)

            // Set icon and title
            iconView.setImageResource(iconRes)
            titleView.text = title

            // Add click listener if needed
            customView.setOnClickListener {
                val currentActivity = this::class.java
                when (title) {
                    "Admin Login" -> {
                        if (!currentActivity.equals(TraineeAdminLogin::class.java)) {
                            val intent = Intent(this, TraineeAdminLogin::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }

                    "Client Login" -> {
                        if (!currentActivity.equals(ClientDashboard::class.java)) {
                            val intent = Intent(this, ClientLogin::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }

                    "Intern/Emp Login" -> {
                        if (!currentActivity.equals(InternDashboard::class.java)) {
                            val intent = Intent(this, InternLogin::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }

                    "Trainee Login" -> {
                        if (currentActivity != TraineeDashboard::class.java) {
                            val intent = Intent(this, TraineeLogin::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }

                    "Rate Us" -> {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$packageName")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            )
                            startActivity(intent)
                        }
                    }

                    "Share" -> {
                        // Share the predefined message
                        val message =
                            "Makes360 - Your IT Partner\n\nSince 2018, Makes360 has delivered 114+ projects across 12+ industries. We specialize in brand building, marketing, and business consulting with lifetime free maintenance and 24/7 support. Let’s drive your digital success!\n\nDownload our app from Google Play Store. Click the link below:\n" +
                                    "https://play.google.com/store/apps/details?id=$packageName"

                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, message)

                        startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                }
                mBinding.drawerLayout.closeDrawer(GravityCompat.START)
            }

            // Add the custom view to the container
            menuParent.addView(customView)
        }

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

        mBinding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (email != null) {
                    setUpViews()
                    fetchTraineeDetails(email)
                }
            } else {
                showNoInternet()
            }
            mBinding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setUpViews() {
        imageSlider()
        announcementList()
        footer()
    }

    private fun footer() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        mBinding.footerTextView.text = getString(R.string.footer_text, currentYear)
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

        // Configure WebView settings
        with(announcementWebView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            textZoom = 220
        }

        announcementWebView.setOnLongClickListener {
            // Do nothing on long press
            true
        }


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

                        // Find the announcement with the maximum ID
                        val recentAnnouncement = (0 until announcements.length())
                            .map { announcements.getJSONObject(it) }
                            .maxByOrNull { it.getInt("id") }

                        withContext(Dispatchers.Main) {
                            hideLoader()

                            if (recentAnnouncement != null) {
                                val id = recentAnnouncement.getString("id")
                                val message = recentAnnouncement.getString("message")
                                val readBy = recentAnnouncement.getString("read_by")
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
                                cardView.visibility = View.GONE
                                announcementWebView.loadData(
                                    "<h1>No announcements available.</h1>",
                                    "text/html",
                                    "UTF-8"
                                )
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
                        announcementWebView(parsedData.email)
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

        traineeDetailsList.clear()

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
                icon = if (details.offerLetter == "0") {
                    R.drawable.ic_question_mark
                } else {
                    R.drawable.ic_offer_letter
                }
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
                title = if (details.updated == "0") {
                    "Update Profile"
                } else {
                    "Profile Update"
                },
                icon = if (details.updated == "0") {
                    R.drawable.ic_question_mark
                } else {
                    R.drawable.ic_update
                }
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
                icon = R.drawable.ic_info,
                email = details.email
            )
        )

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Support",
                icon = R.drawable.ic_support
            )
        )

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Feedback",
                icon = R.drawable.ic_feedback
            )
        )

        traineeDetailsList.add(
            TraineeDetailsData(
                title = "Attendance",
                icon = R.drawable.ic_attendance
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
            updated = jsonResponse.optString("updated", "Unknown"),
            lastLogin = jsonResponse.optString("lastLogin", "Unknown")
        )
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

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class TraineeDetails(
        val name: String,
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
        val updated: String,
        val lastLogin: String
    )

    override fun onBackPressed() {
        if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
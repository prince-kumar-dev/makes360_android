package com.makes360.app.ui.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.client.ClientDetailsAdapter
import com.makes360.app.adapters.client.ClientProjectListAdapter
import com.makes360.app.databinding.ActivityClientDashboardBinding
import com.makes360.app.models.client.ClientDetailsData
import com.makes360.app.models.client.ProjectListDetailsData
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

class ClientDashboard : BaseActivity() {

    private lateinit var progressOverlay: View
    private lateinit var progressBar: ProgressBar
    private lateinit var mBinding: ActivityClientDashboardBinding
    private var selectedProjectId: String = ""
    private var selectedProjectName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityClientDashboardBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.clientDashboardToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        progressOverlay = findViewById(R.id.progressOverlay)
        progressBar = findViewById(R.id.progressBar)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")
        val custId = 4320561
        val firstName = intent.getStringExtra("FIRST_NAME")

        mBinding.titleToolbarTxt.text = "Hi ${firstName}"

        if (email != null && custId != null) {
            showLoader()
            setUpViews(email)
            fetchProjectDetails(custId)
            announcementWebView(custId.toString())
        } else {
            showToast("Invalid email! Please try again.")
        }

    }

    private fun showLoader() {
        progressOverlay.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressOverlay.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun setUpViews(email: String) {
        setUpImageSlider()
        setUpClientProfileAvatarImageView(email)
        setUpClientDetails()
        announcementList()
    }

    private fun setUpClientDetails() {

        val projectId = selectedProjectId

        val detailsList = listOf(
            ClientDetailsData(
                icon = R.drawable.ic_nav_logo,
                title = "Project Details",
                projectId = projectId
            ),
            ClientDetailsData(
                icon = R.drawable.ic_nav_logo,
                title = "Project Assets",
                projectId = projectId,
                projectName = selectedProjectName
            ),
            ClientDetailsData(
                icon = R.drawable.ic_nav_logo,
                title = "Contact Log",
                projectId = projectId
            ),
            ClientDetailsData(
                icon = R.drawable.ic_nav_logo,
                title = "Service History",
                projectId = projectId
            ),
            ClientDetailsData(
                icon = R.drawable.ic_nav_logo,
                title = "Credentials",
                projectId = projectId
            ),
        )

        val adapter = ClientDetailsAdapter(this, detailsList)
        mBinding.detailsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        mBinding.detailsRecyclerView.adapter = adapter
    }

    private fun setUpProjectListRecyclerView(projectDetailsList: List<ProjectList>) {

        val projectsList = projectDetailsList.map { project ->
            ProjectListDetailsData(
                title = project.projectName,
                details = mapOf(
                    "Project ID" to project.projectId.toString(),
                ),
                icon = R.drawable.ic_project_list
            )
        }

        val adapter = ClientProjectListAdapter(this, projectsList) { projectId, projectName ->
            selectedProjectId = projectId
            selectedProjectName = projectName
            setUpClientDetails()
        }

        mBinding.projectListRV.layoutManager = LinearLayoutManager(this)
        mBinding.projectListRV.adapter = adapter
    }


    private fun setUpClientProfileAvatarImageView(email: String) {
        val imageView = findViewById<ImageView>(R.id.clientProfileAvatarImageView)

        imageView.setOnClickListener {
            val intent = Intent(this, ClientProfile::class.java)
            intent.putExtra("EMAIL", email)
            startActivity(intent)
        }
    }

    private fun setUpImageSlider() {
        // Initialize the ImageSlider
        val imageSlider = findViewById<ImageSlider>(R.id.imageSlider)
        val imageList = ArrayList<SlideModel>()

        for (i in 1 until 4) {
            // Append a timestamp to force reload
            val imageUrl =
                "https://www.makes360.com/application/makes360/client/slider/image$i.png?timestamp=${System.currentTimeMillis()}"
            imageList.add(SlideModel(imageUrl))
        }

        imageSlider.setImageList(imageList, ScaleTypes.FIT)
    }

    private fun announcementWebView(custId: String) {
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
                    URL("https://www.makes360.com/application/makes360/client/announcement.php")
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
                                val isRead = readBy.split(",").contains(custId)

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
                                            this@ClientDashboard,
                                            R.color.material_core_light_green
                                        )
                                    )
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@ClientDashboard,
                                            R.color.primary_text
                                        )
                                    )
                                    cardViewText.text = "Great! You Read It"
                                    cardView.isClickable = false
                                } else {
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@ClientDashboard,
                                            R.color.white
                                        )
                                    )
                                    cardView.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            this@ClientDashboard,
                                            R.color.colorPrimary
                                        )
                                    )
                                    cardView.isClickable = true
                                    cardView.setOnClickListener {
                                        markAsRead(custId, id, cardView, cardViewText)
                                    }
                                }
                            } else {
                                val id = announcementToShow?.getString("id")
                                val message = announcementToShow?.getString("message")
                                val readBy = announcementToShow?.getString("read_by")
                                val isRead = readBy?.split(",")?.contains(custId)

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
                                            this@ClientDashboard,
                                            R.color.material_core_light_green
                                        )
                                    )
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@ClientDashboard,
                                            R.color.primary_text
                                        )
                                    )
                                    cardViewText.text = "Great! You Read It"
                                    cardView.isClickable = false
                                } else {
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@ClientDashboard,
                                            R.color.white
                                        )
                                    )
                                    cardView.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            this@ClientDashboard,
                                            R.color.colorPrimary
                                        )
                                    )
                                    cardView.isClickable = true
                                    cardView.setOnClickListener {
                                        if (id != null) {
                                            markAsRead(custId, id, cardView, cardViewText)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            hideLoader()
                            mBinding.markAsReadCardView.visibility = View.GONE
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
        custId: String,
        announcementId: String,
        cardView: CardView,
        cardViewText: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/client/mark-as-read.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                val requestBody =
                    "custId=$custId&announcementId=$announcementId"
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
                                    this@ClientDashboard,
                                    R.color.material_core_light_green
                                )
                            )
                            cardViewText.setTextColor(
                                ContextCompat.getColor(
                                    this@ClientDashboard,
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
            intent.putExtra("CHECK_CANDIDATE", "Client")
            startActivity(intent)
        }
    }


    private fun fetchProjectDetails(custId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/client/project-list.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                // Write custId to request body
                val requestBody = "cust_id=$custId"
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
                        setUpProjectListRecyclerView(projectDetailsList)
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

    private fun parseProjectResponse(response: String): List<ProjectList> {
        val projectList = mutableListOf<ProjectList>()
        val jsonResponse = JSONObject(response)

        if (jsonResponse.optBoolean("success", false)) {
            val projectsArray = jsonResponse.optJSONArray("projects")
            if (projectsArray != null) {
                for (i in 0 until projectsArray.length()) {
                    val projectJson = projectsArray.getJSONObject(i)
                    val project = ProjectList(
                        projectId = projectJson.optInt("project_id"),
                        projectName = projectJson.optString("project_name", "Unknown")
                    )
                    projectList.add(project)
                }
            }
        } else {
            runOnUiThread {
                showToast(jsonResponse.optString("error", "Unknown error occurred."))
            }
        }

        return projectList
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class ProjectList(
        val projectId: Int,
        val projectName: String
    )
}
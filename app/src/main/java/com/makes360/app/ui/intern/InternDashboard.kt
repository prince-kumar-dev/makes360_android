package com.makes360.app.ui.intern

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.BaseActivity
import com.makes360.app.MainActivity
import com.makes360.app.R
import com.makes360.app.adapters.DownloadContentAdapter
import com.makes360.app.adapters.InternDetailsAdapter
import com.makes360.app.models.InternDetailsRV
import com.makes360.app.adapters.RoadmapStepAdapter
import com.makes360.app.models.DownloadContent
import com.makes360.app.models.RoadmapStep
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

class InternDashboard : BaseActivity() {

    private lateinit var progressOverlay: View
    private lateinit var progressBar: ProgressBar
    private var internDetailsList = mutableListOf<InternDetailsRV>()
    private lateinit var internDetailsAdapter: InternDetailsAdapter
    private lateinit var adapter: DownloadContentAdapter
    private val offerRoot =
        "https://www.makes360.com/backend/admin/file/internship/resources/offer-letter/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        setContentView(R.layout.activity_intern_dashboard)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.homeToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        progressOverlay = findViewById(R.id.progressOverlay)
        progressBar = findViewById(R.id.progressBar)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")

        if (email != null) {
            showLoader()
            fetchInternDetails(email)
        } else {
            showToast("Invalid email! Please try again.")
        }
        setUpViews()
    }

    private fun setUpViews() {
        imageSlider()
        downloadContent()
        announcementList()
    }

    private fun announcementWebView(certificateNumber: String) {
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
                    URL("https://www.makes360.com/application/makes360/internship/announcement.php")
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
                                val isRead = readBy.split(",").contains(certificateNumber)

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
                                            this@InternDashboard,
                                            R.color.material_core_light_green
                                        )
                                    )
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@InternDashboard,
                                            R.color.primary_text
                                        )
                                    )
                                    cardViewText.text = "Great! You Read It"
                                    cardView.isClickable = false
                                } else {
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@InternDashboard,
                                            R.color.white
                                        )
                                    )
                                    cardView.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            this@InternDashboard,
                                            R.color.colorPrimary
                                        )
                                    )
                                    cardView.isClickable = true
                                    cardView.setOnClickListener {
                                        markAsRead(certificateNumber, id, cardView, cardViewText)
                                    }
                                }
                            } else {
                                val id = announcementToShow?.getString("id")
                                val message = announcementToShow?.getString("message")
                                val readBy = announcementToShow?.getString("read_by")
                                val isRead = readBy?.split(",")?.contains(certificateNumber)

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
                                            this@InternDashboard,
                                            R.color.material_core_light_green
                                        )
                                    )
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@InternDashboard,
                                            R.color.primary_text
                                        )
                                    )
                                    cardViewText.text = "Great! You Read It"
                                    cardView.isClickable = false
                                } else {
                                    cardViewText.setTextColor(
                                        ContextCompat.getColor(
                                            this@InternDashboard,
                                            R.color.white
                                        )
                                    )
                                    cardView.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            this@InternDashboard,
                                            R.color.colorPrimary
                                        )
                                    )
                                    cardView.isClickable = true
                                    cardView.setOnClickListener {
                                        if (id != null) {
                                            markAsRead(
                                                certificateNumber,
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
        certificateNumber: String,
        announcementId: String,
        cardView: CardView,
        cardViewText: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/internship/mark-as-read.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                val requestBody =
                    "certificateNumber=$certificateNumber&announcementId=$announcementId"
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
                                    this@InternDashboard,
                                    R.color.material_core_light_green
                                )
                            )
                            cardViewText.setTextColor(
                                ContextCompat.getColor(
                                    this@InternDashboard,
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
            intent.putExtra("CHECK_CANDIDATE", "Intern")
            startActivity(intent)
        }
    }

    private fun downloadContent() {
        val downloadContentRV = findViewById<RecyclerView>(R.id.downloadContentRV)

        val downloadContentList = listOf(
            DownloadContent(
                heading = "Intern Handbook",
                description = "Conduct rules for all Makes360 interns, coworkers, and subcontractors.",
                iconResId = R.drawable.intern_handbook,
                url = "https://www.makes360.com/application/makes360/internship/files/intern-handbook.pdf"
            ),
            DownloadContent(
                heading = "Perks & Benefits",
                description = "Detailed instructions for completing assigned projects effectively.",
                iconResId = R.drawable.ic_perks,
                url = "https://www.makes360.com/application/makes360/internship/files/perks-and-benefits.pdf"
            ),
            DownloadContent(
                heading = "Milestone Benefits",
                description = "Recognition, rewards, and growth opportunities for achieving milestones at Makes360.",
                iconResId = R.drawable.ic_goals,
                url = "https://www.makes360.com/application/makes360/internship/files/milestone-benefits.pdf"
            ),
            DownloadContent(
                heading = "Attendance Policy & Leave",
                description = "Regular attendance is required. Leaves must be pre-approved, except in emergencies.",
                iconResId = R.drawable.ic_attendance,
                url = "https://www.makes360.com/application/makes360/internship/files/attendence-policy.pdf"
            ),
            DownloadContent(
                heading = "Certificate Policy",
                description = "Internship certificate is awarded upon completing all tasks and meeting performance standards.",
                iconResId = R.drawable.ic_certificate,
                url = "https://www.makes360.com/application/makes360/internship/files/certificate-policy.pdf"
            ),
            DownloadContent(
                heading = "Reimbursement Form",
                description = "Use this form to claim reimbursement for work related expenses.",
                iconResId = R.drawable.ic_reimbursement,
                url = "https://www.makes360.com/application/makes360/internship/files/reimbursement-form.docx"
            )

        )

        adapter = DownloadContentAdapter(downloadContentList)
        downloadContentRV.layoutManager = LinearLayoutManager(this)
        downloadContentRV.adapter = adapter
    }

    private fun internDetailsRecyclerView(details: InternDetails) {
        val root = "https://www.makes360.com"
        val internDetailsRecyclerView =
            findViewById<RecyclerView>(R.id.detailsRecyclerView)

        internDetailsList.add(
            InternDetailsRV(
                icon = R.drawable.intern_boy,
                title = "Profile Details",
                email = details.email,
                profileLink = "profile"
            )
        )
        internDetailsList.add(
            InternDetailsRV(
                icon = R.drawable.ic_offer_letter,
                title = "Offer Letter",
                email = details.email,
                offerLetterLink = if (details.offerLetterLink == "0" || details.offerLetterLink.isNullOrEmpty()) {
                    ""
                } else {
                    offerRoot + details.offerLetterLink
                },
                applicationStatus = details.applicationStatus
            )
        )
        internDetailsList.add(
            InternDetailsRV(
                icon = R.drawable.ic_resume,
                title = "Resume",
                email = details.email,
                resumeLink = if (details.resumeLink == "null" || details.resumeLink.isNullOrEmpty()) {
                    ""
                } else {
                    root + details.resumeLink
                },
                applicationStatus = details.applicationStatus
            )
        )
        internDetailsList.add(
            InternDetailsRV(
                icon = R.drawable.ic_video_resume,
                title = "Video Resume",
                email = details.email,
                videoResumeLink = if (details.videoResumeLink.isNullOrEmpty()) {
                    ""
                } else {
                    root + details.videoResumeLink
                },
                applicationStatus = details.applicationStatus
            )
        )
        internDetailsList.add(
            InternDetailsRV(
                icon = R.drawable.money,
                title = "Stipend",
                email = details.email,
                stipendLink = "stipend"
            )
        )
        internDetailsList.add(
            InternDetailsRV(
                R.drawable.ic_certificate,
                title = "Certificate",
                email = details.email,
                certificateLink = details.certificateStatus,
                applicationStatus = details.applicationStatus
            )
        )
        internDetailsAdapter = InternDetailsAdapter(this, internDetailsList)
        internDetailsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        internDetailsRecyclerView.adapter = internDetailsAdapter
    }

    private fun showLoader() {
        progressOverlay.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        progressOverlay.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun imageSlider() {
        // Initialize the ImageSlider
        val imageSlider = findViewById<ImageSlider>(R.id.imageSlider)
        val imageList = ArrayList<SlideModel>()

        for (i in 1 until 4) {
            // Append a timestamp to force reload
            val imageUrl =
                "https://www.makes360.com/application/makes360/internship/slider/image$i.png?timestamp=${System.currentTimeMillis()}"
            imageList.add(SlideModel(imageUrl))
        }

        imageSlider.setImageList(imageList, ScaleTypes.FIT)
    }

    private fun fetchInternDetails(email: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/internship/intern-profile-details.php")
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
                        updateTrackingUI(parsedData)
                        internDetailsRecyclerView(parsedData)
                        announcementWebView(parsedData.certificateNumber)
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

    private fun parseResponse(response: String): InternDetails {
        val jsonResponse = JSONObject(response)
        val applicationStatus = jsonResponse.optInt("application_status", -1)
        val email = jsonResponse.optString("email", "Unknown")
        val name = jsonResponse.optString("name", "Unknown")
        val dob = jsonResponse.optString("dob", "Unknown")
        val certificateNumber = jsonResponse.optString("certificate_number", "Unknown")
        val applyDate = jsonResponse.optString("apply_date", "Unknown")
        val shortListedDate = jsonResponse.optString("shortlisted_date", "Unknown")
        val callForInterviewDate = jsonResponse.optString("call_for_interview_date", "Unknown")
        val joiningDate = jsonResponse.optString("join_date", "Unknown")
        val passedOutDate = jsonResponse.optString("complete_date", "Unknown")
        val certificateStatus = jsonResponse.optString("certificate_status", "Unknown")
        val offerLetterLink = jsonResponse.optString("offer_letter_link", "Unknown")
        val videoResumeLink = jsonResponse.optString("video_resume_link", "Unknown")
        val resumeLink = jsonResponse.optString("resume_link", "Unknown")
        return InternDetails(
            applicationStatus,
            email,
            name,
            dob,
            certificateNumber,
            applyDate,
            shortListedDate,
            callForInterviewDate,
            joiningDate,
            passedOutDate,
            certificateStatus,
            offerLetterLink,
            videoResumeLink,
            resumeLink
        )
    }

    private fun updateTrackingUI(details: InternDetails) {

        val roadmapRecyclerView = findViewById<RecyclerView>(R.id.roadmapRecyclerView)
        val currentState = details.applicationStatus

        // Define roadmap steps
        val steps = listOf(
            RoadmapStep(
                -1,
                "Journey Begin",
                "Welcome to your internship! Learn, grow, and explore new challenges. Stay curious and make the most of every moment!",
                true,
                cardViewBgColor = R.color.material_flat_turquoise
            ),
            RoadmapStep(
                0,
                "Applied for Internship",
                "Congratulations on starting your journey! Growth and success await - embrace the path ahead!",
                currentState >= 0,
                currentState,
                appliedDate = details.applyDate,
                cardViewBgColor = R.color.material_flat_blue
            ),
            RoadmapStep(
                1,
                "Shortlisted",
                "Congrats on being shortlisted! Please upload your video and PDF resumes to proceed.",
                currentState >= 1,
                currentState,
                shortListedDate = details.shortListedDate,
                cardViewBgColor = R.color.material_flat_amethyst
            ),
            RoadmapStep(
                2,
                "Call for Interview",
                "Congratulations! Your resume and video resume are shortlisted. We’ll invite you for an interview soon.",
                currentState >= 2,
                currentState,
                interviewCallDate = details.callForInterviewDate,
                cardViewBgColor = R.color.colorPrimary
            ),
            RoadmapStep(
                3,
                "Rejected",
                "Thank you for your time and effort. While we’re not moving forward at this moment",
                currentState == 3,
                currentState,
                cardViewBgColor = R.color.material_flat_red_dark
            ),
            RoadmapStep(
                4,
                "Offer Letter Signed",
                "Congratulations! We're excited to offer you an internship. We look forward to your amazing contributions!",
                currentState >= 4,
                currentState,
                offerLetterLink = offerRoot + details.offerLetterLink,
                cardViewBgColor = R.color.material_flat_carrot
            ),
            RoadmapStep(
                5,
                "Doing Internship",
                "Welcome to the team! We're excited to have you as our intern and can’t wait to see you grow!",
                currentState >= 5,
                currentState,
                joiningDate = details.joiningDate,
                cardViewBgColor = R.color.material_flat_red
            ),
            RoadmapStep(
                6,
                "Completed",
                "Congrats on finishing your internship! We're proud of you. Best wishes for your next chapter!",
                currentState >= 6,
                currentState,
                passedOutDate = details.passedOutDate,
                cardViewBgColor = R.color.material_core_green
            )
        )

        // Filter out the "Rejected" step if the current state is not 3
        val filteredSteps = if (currentState == 3) {
            steps
        } else {
            steps.filter { it.title != "Rejected" }
        }

        // Set up RecyclerView
        val adapter = RoadmapStepAdapter(filteredSteps)
        roadmapRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        roadmapRecyclerView.adapter = adapter

        // Scroll to current step
        val currentStepIndex =
            filteredSteps.indexOfFirst { details.applicationStatus == it.stepNumber }

        roadmapRecyclerView.post {
            val layoutManager = roadmapRecyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.let {
                val itemWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._200sdp)
                val screenWidth = resources.displayMetrics.widthPixels
                val offset = (screenWidth / 2) - (itemWidth / 2)

                layoutManager.startSmoothScroll(object :
                    LinearSmoothScroller(roadmapRecyclerView.context) {
                    override fun getVerticalSnapPreference(): Int = SNAP_TO_START
                    override fun calculateDxToMakeVisible(view: View?, snapPreference: Int): Int {
                        return super.calculateDxToMakeVisible(view, snapPreference) - offset
                    }
                }.apply { targetPosition = currentStepIndex })
            }
        }

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.homeToolbar)
        val titleToolbarTxt = toolbar.findViewById<TextView>(R.id.titleToolbarTxt)
        val name = details.name.substringBefore(" ") // Extract name up to the first space
        titleToolbarTxt.text = "Hi, $name!"
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class InternDetails(
        val applicationStatus: Int,
        val email: String,
        val name: String,
        val dob: String,
        val certificateNumber: String,
        val applyDate: String,
        val shortListedDate: String,
        val callForInterviewDate: String,
        val joiningDate: String,
        val passedOutDate: String,
        val certificateStatus: String,
        val offerLetterLink: String,
        val videoResumeLink: String,
        val resumeLink: String
    )
}
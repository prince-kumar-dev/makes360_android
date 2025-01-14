package com.makes360.app.ui.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.R
import com.makes360.app.adapters.client.ClientDetailsAdapter
import com.makes360.app.adapters.client.ClientProjectListAdapter
import com.makes360.app.databinding.ActivityClientDashboardBinding
import com.makes360.app.models.client.ClientDetailsData
import com.makes360.app.models.client.ProjectListDetailsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class ClientDashboard : AppCompatActivity() {

    private lateinit var progressOverlay: View
    private lateinit var progressBar: ProgressBar
    private lateinit var mBinding: ActivityClientDashboardBinding
    private var selectedProjectId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        val custId = 5934062
        val firstName = intent.getStringExtra("FIRST_NAME")

        mBinding.titleToolbarTxt.text = "Hi ${firstName}"

        if (email != null && custId != null) {
            showLoader()
            setUpViews(email)
            fetchProjectDetails(custId.toInt())
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
                projectId = projectId
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

        val adapter = ClientProjectListAdapter(this, projectsList) { projectId ->
            selectedProjectId = projectId
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
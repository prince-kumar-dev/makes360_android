package com.makes360.app.ui.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.R

class ClientDashboard : AppCompatActivity() {

    private lateinit var progressOverlay: View
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_dashboard)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.clientDashboardToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        progressOverlay = findViewById(R.id.progressOverlay)
        progressBar = findViewById(R.id.progressBar)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")

        if (email != null) {
            // showLoader()
            setUpViews(email)
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

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
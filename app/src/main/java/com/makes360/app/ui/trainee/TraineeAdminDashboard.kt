package com.makes360.app.ui.trainee

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.databinding.ActivityTraineeAdminDashboardBinding
import com.makes360.app.databinding.ActivityTraineeLeaderboardBinding
import com.makes360.app.util.NetworkUtils

class TraineeAdminDashboard : BaseActivity() {

    private lateinit var mBinding: ActivityTraineeAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityTraineeAdminDashboardBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setUpTraineePointsAdminWebView()

        mBinding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                setUpTraineePointsAdminWebView()
            } else {
                showNoInternet()
            }
            mBinding.swipeRefreshLayout.isRefreshing = false
        }

        setUpLogOut()

        mBinding.backImageView.setOnClickListener {
            finish()
        }
    }

    private fun setUpLogOut() {
        mBinding.logOutImageView.setOnClickListener {
            val sharedPreferences = getSharedPreferences("AdminLoginPref", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear() // Clears all data
            editor.apply()

            Toast.makeText(this, "Log out Successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, TraineeAdminLogin::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setUpTraineePointsAdminWebView() {
        mBinding.traineePointsAdminWebView.setOnLongClickListener {
            // Do nothing on long press
            true
        }

        // Configure WebView settings
        val webSettings: WebSettings = mBinding.traineePointsAdminWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        // Show loader before loading content
        showLoader()

        // Configure WebViewClient with loading callbacks
        mBinding.traineePointsAdminWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Show loader when the page starts loading
                showLoader()
            }

            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Hide loader when the page finishes loading
                hideLoader()
            }
        }

        // Load content
        mBinding.traineePointsAdminWebView.loadUrl("https://www.makes360.com/application/makes360/trainee/admin-panel.php")
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
}
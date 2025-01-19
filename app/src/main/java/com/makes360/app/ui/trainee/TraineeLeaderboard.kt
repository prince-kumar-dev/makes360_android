package com.makes360.app.ui.trainee

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.databinding.ActivityTraineeLeaderboardBinding
import com.makes360.app.databinding.ActivityTraineeProfileBinding
import com.makes360.app.util.NetworkUtils

class TraineeLeaderboard : BaseActivity() {

    private lateinit var mBinding: ActivityTraineeLeaderboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityTraineeLeaderboardBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setUpLeaderboardWebView()
        setUpBackButton()
    }

    private fun setUpLeaderboardWebView() {
        mBinding.leaderboardWebView.setOnLongClickListener {
            // Do nothing on long press
            true
        }

        // Configure WebView settings
        val webSettings: WebSettings = mBinding.leaderboardWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        // Show loader before loading content
        showLoader()

        // Configure WebViewClient with loading callbacks
        mBinding.leaderboardWebView.webViewClient = object : WebViewClient() {
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
        mBinding.leaderboardWebView.loadUrl("https://www.makes360.com/application/makes360/trainee/leaderboard.php")
    }


    private fun showLoader() {
        mBinding.progressOverlay.visibility = View.VISIBLE
        mBinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        mBinding.progressOverlay.visibility = View.GONE
        mBinding.progressBar.visibility = View.GONE
    }

    private fun setUpBackButton() {
        mBinding.backImageView.setOnClickListener {
            finish()
        }
    }

}
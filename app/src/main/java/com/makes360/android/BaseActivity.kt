package com.makes360.android

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.makes360.android.util.NetworkUtils

open class BaseActivity : AppCompatActivity() {

    private lateinit var noInternetView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate a common "No Internet" view in all activities
        noInternetView = layoutInflater.inflate(R.layout.layout_no_internet, null)
    }

    override fun setContentView(layoutResID: Int) {
        val frameLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        layoutInflater.inflate(layoutResID, frameLayout, true)
        setContentView(frameLayout)
        checkInternet()
    }

    private fun checkInternet() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            showNoInternet()
        } else {
            hideNoInternet()
        }
    }

    private fun showNoInternet() {
        if (!this::noInternetView.isInitialized) return
        (findViewById<FrameLayout>(android.R.id.content)!!).addView(noInternetView)
    }

    private fun hideNoInternet() {
        if (!this::noInternetView.isInitialized) return
        (findViewById<FrameLayout>(android.R.id.content)!!).removeView(noInternetView)
    }

    override fun onResume() {
        super.onResume()
        checkInternet() // Re-check on resume
    }
}
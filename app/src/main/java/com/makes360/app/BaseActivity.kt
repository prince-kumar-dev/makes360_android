package com.makes360.app

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.makes360.app.util.NetworkUtils

open class BaseActivity : AppCompatActivity() {

    private lateinit var noInternetView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate a common "No Internet" view in all activities
        noInternetView = layoutInflater.inflate(R.layout.layout_no_internet, null)

        // Set up refresh button listener
        noInternetView.findViewById<MaterialCardView>(R.id.refreshContentCardView)
            .setOnClickListener {
                onRefreshContent()
            }
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

    fun hideNoInternet() {
        if (!this::noInternetView.isInitialized) return
        (findViewById<FrameLayout>(android.R.id.content)!!).removeView(noInternetView)
    }

    override fun onResume() {
        super.onResume()
        checkInternet() // Re-check on resume
    }

    /**
     * Called when the Refresh Content button is clicked.
     */
    open fun onRefreshContent() {
        // Re-check internet and reload content
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            recreate() // Reload the current activity
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
        }
    }
}
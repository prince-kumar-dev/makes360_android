package com.makes360.app

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.makes360.app.util.NetworkUtils
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

open class BaseActivity : AppCompatActivity() {

    private lateinit var noInternetView: View
    private var swipeRefreshLayout: SwipeRefreshLayout? = null // Nullable to avoid conflicts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate a common "No Internet" view
        noInternetView = layoutInflater.inflate(R.layout.layout_no_internet, null)

        // Set up the refresh button listener
        noInternetView.findViewById<MaterialCardView>(R.id.refreshContentCardView)
            .setOnClickListener {
                onRefreshContent()
            }
    }

    override fun setContentView(layoutResID: Int) {
        // Determine if the child activity uses SwipeRefreshLayout
        val useSwipeRefreshLayout = shouldUseSwipeRefreshLayout()

        if (useSwipeRefreshLayout) {
            // Wrap the activity content in a SwipeRefreshLayout
            swipeRefreshLayout = SwipeRefreshLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setOnRefreshListener {
                    onRefreshContent() // Trigger refresh when pulled
                }
            }

            // Inflate the activity's layout into the SwipeRefreshLayout
            val frameLayout = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            layoutInflater.inflate(layoutResID, frameLayout, true)
            swipeRefreshLayout?.addView(frameLayout)
            super.setContentView(swipeRefreshLayout)
        } else {
            // Set content view normally
            super.setContentView(layoutResID)
        }

        // Check internet connection initially
        checkInternet()
    }

    private fun checkInternet() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            showNoInternet()
        } else {
            hideNoInternet()
        }
    }

    fun showNoInternet() {
        if (!this::noInternetView.isInitialized) return
        (findViewById<FrameLayout>(android.R.id.content)!!).addView(noInternetView)
        swipeRefreshLayout?.isEnabled = true // Enable pull-to-refresh even when offline
    }

    fun hideNoInternet() {
        if (!this::noInternetView.isInitialized) return
        (findViewById<FrameLayout>(android.R.id.content)!!).removeView(noInternetView)
        swipeRefreshLayout?.isRefreshing = false // Stop the loading animation
    }

    override fun onResume() {
        super.onResume()
        checkInternet() // Recheck connection when resuming
    }

    /**
     * Called when the Refresh Content button or Pull-to-Refresh is triggered.
     */
    open fun onRefreshContent() {
        // Recheck internet and reload content
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            recreate() // Reload the current activity
        } else {
            swipeRefreshLayout?.isRefreshing = false // Stop refreshing if no internet
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Determines if the SwipeRefreshLayout should be enabled for this activity.
     */
    open fun shouldUseSwipeRefreshLayout(): Boolean {
        return false // Default is no SwipeRefreshLayout; override in child if needed
    }
}
package com.makes360.app.ui.client

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.Volley
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.adapters.client.CredentialAdapter
import com.makes360.app.databinding.ActivityClientCredentialsBinding
import com.makes360.app.databinding.ActivityContactLogBinding
import com.makes360.app.models.client.CredentialData
import com.makes360.app.util.NetworkUtils

class ClientCredentials : BaseActivity() {

    private lateinit var mBinding: ActivityClientCredentialsBinding
    // private lateinit var requestQueue: RequestQueue
    private val credentials = ArrayList<CredentialData>()
    private lateinit var adapter: CredentialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_credentials)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityClientCredentialsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.credentialsToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
//        requestQueue = Volley.newRequestQueue(this)

        mBinding.credentialsRV.layoutManager = LinearLayoutManager(this)

        // Get the email passed from the previous activity
        val projectId = intent.getStringExtra("PROJECT_ID")
        val projectName = intent.getStringExtra("PROJECT_NAME")

        // Load sample data
        val sampleData = getSampleCredentials()
        adapter = CredentialAdapter(sampleData, this)
        mBinding.credentialsRV.adapter = adapter


        mBinding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (projectId != null && projectName != null) {
                    // fetchProjectContactLog(projectId.toInt())
                }
            } else {
                showNoInternet()
            }
            mBinding.swipeRefreshLayout.isRefreshing = false
        }

        mBinding.backImageView.setOnClickListener {
            finish()
        }
    }

    // Function to generate sample credentials
    private fun getSampleCredentials(): List<CredentialData> {
        return listOf(
            CredentialData("Email", "user@example.com"),
            CredentialData("Facebook", "fb_user123"),
            CredentialData("Instagram", "insta_handle"),
            CredentialData("Twitter", "twitter_user"),
            CredentialData("LinkedIn", "linkedin_profile"),
            CredentialData("Github", "github_user"),
            CredentialData("WordPress", "wp_admin"),
            CredentialData("Amazon", "amazon_account"),
            CredentialData("Netflix", "netflix_login"),
            CredentialData("Spotify", "spotify_user"),
            CredentialData("Bank Account", "banking_id")
        )
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
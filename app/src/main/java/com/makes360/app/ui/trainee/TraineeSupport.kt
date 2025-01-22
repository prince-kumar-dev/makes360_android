package com.makes360.app.ui.trainee

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.databinding.ActivityTraineeFeeInfoBinding
import com.makes360.app.databinding.ActivityTraineeSupportBinding
import com.makes360.app.util.NetworkUtils

class TraineeSupport : BaseActivity() {

    private lateinit var mBinding: ActivityTraineeSupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_trainee_support)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityTraineeSupportBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setUpBackButton()
    }

    private fun setUpBackButton() {
        findViewById<ImageView>(R.id.backImageView).setOnClickListener {
            finish()
        }
    }
}
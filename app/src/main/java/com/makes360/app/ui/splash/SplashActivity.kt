package com.makes360.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.makes360.app.MainActivity
import com.makes360.app.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logo)
        val quote = findViewById<TextView>(R.id.txtSplashQuote)
        val footer = findViewById<TextView>(R.id.txtSplashFooter)

        // Load animations
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        val quoteAnimation = AnimationUtils.loadAnimation(this, R.anim.txtsplashquote)
        val footerAnimation = AnimationUtils.loadAnimation(this, R.anim.txtsplashquote)

        // Apply animations
        logo.startAnimation(logoAnimation)
        quote.startAnimation(quoteAnimation)
        footer.startAnimation(footerAnimation)

        // Navigate to MainActivity after a delay
        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000) // 3 seconds delay
    }
}
package com.makes360.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.adapters.CompanyProfileAdapter
import com.makes360.app.databinding.ActivityMainBinding
import com.makes360.app.models.CompanyProfileData
import com.makes360.app.ui.client.ClientLogin
import com.makes360.app.ui.intern.InternLogin
import com.makes360.app.util.NetworkUtils
import java.util.Calendar

open class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var companyProfileList = mutableListOf<CompanyProfileData>()
    private lateinit var adapter: CompanyProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Set up ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Add hamburger menu icon
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeAsUpIndicator(R.drawable.ic_menu) // Use your hamburger icon

        binding.navigationView.itemIconTintList = null

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                }
                R.id.nav_intern -> {
                    // Start InternLogin activity
                    val intent = Intent(this, InternLogin::class.java)
                    startActivity(intent)
                }
                R.id.nav_client -> {
                    val intent = Intent(this, ClientLogin::class.java)
                    startActivity(intent)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START) // Close drawer after item click
            true
        }

        // Register network callback
        NetworkUtils.registerNetworkCallback(this) {
            runOnUiThread {
                refreshContent()
            }
        }

        setUpViews()
    }

    private fun setUpViews() {
        setUpImageSlider()
        setUpRecyclerView()
        setUpCompanyProfileList()
        setUpInternLoginBtn()
        setUpClientLoginBtn()
        setUpFooter()
    }

    private fun setUpClientLoginBtn() {
        binding.clientLoginCardView.setOnClickListener {
            val intent = Intent(this, ClientLogin::class.java)
            startActivity(intent)
        }
    }

    private fun setUpInternLoginBtn() {
        binding.internLoginCardView.setOnClickListener {
            val intent = Intent(this, InternLogin::class.java)
            startActivity(intent)
        }
    }

    private fun setUpFooter() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.homeFooterTextView.text = "$currentYear © AGI Innovations Makes360 Private Limited"
    }

    private fun setUpCompanyProfileList() {
        companyProfileList.add(CompanyProfileData("Services", R.drawable.ic_service, "https://www.makes360.com/services/"))
        companyProfileList.add(CompanyProfileData("Our Work", R.drawable.ic_our_work, "https://www.makes360.com/portfolio/"))
        companyProfileList.add(CompanyProfileData("Testimonial", R.drawable.ic_rating, "https://www.makes360.com/testimonials/"))
        companyProfileList.add(CompanyProfileData("About Us", R.drawable.ic_about_us, "https://www.makes360.com/about/"))
        companyProfileList.add(CompanyProfileData("Internship", R.drawable.ic_internship, "https://www.makes360.com/internship/"))
        companyProfileList.add(CompanyProfileData("Training", R.drawable.ic_training, "https://www.makes360.com/services/"))
    }

    private fun setUpRecyclerView() {
        adapter = CompanyProfileAdapter(this, companyProfileList)
        binding.companyProfileRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.companyProfileRecyclerView.adapter = adapter
    }

    private fun setUpImageSlider() {
        val imageSlider = findViewById<com.denzcoskun.imageslider.ImageSlider>(R.id.homeImageSlider)

        val imageList = ArrayList<SlideModel>()

        for (i in 1 until 4) {
            // Append a timestamp to force reload
            val imageUrl =
                "https://www.makes360.com/application/makes360/slider/image$i.png?timestamp=${System.currentTimeMillis()}"
            imageList.add(SlideModel(imageUrl))
        }

        imageSlider.setImageList(imageList, ScaleTypes.FIT)
    }

    private fun refreshContent() {
        recreate()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
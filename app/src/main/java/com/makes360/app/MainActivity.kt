package com.makes360.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.adapters.CompanyProfileAdapter
import com.makes360.app.databinding.ActivityMainBinding
import com.makes360.app.models.CompanyProfileData
import com.makes360.app.ui.client.ClientLogin
import com.makes360.app.ui.intern.InternLogin
import com.makes360.app.ui.trainee.TraineeAdminDashboard
import com.makes360.app.ui.trainee.TraineeAdminLogin
import com.makes360.app.ui.trainee.TraineeLogin
import com.makes360.app.util.NetworkUtils
import java.util.Calendar

open class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var companyProfileList = mutableListOf<CompanyProfileData>()
    private lateinit var adapter: CompanyProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
            setUpViews()
        }
    }

    private fun loadContent() {
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

        val navigationView = binding.navigationView

        // List of menu items
        val menuItems = listOf(
            Pair(R.drawable.ic_admin, "Admin Login"),
            Pair(R.drawable.ic_man_client, "Client Login"),
            Pair(R.drawable.ic_nav_intern, "Intern/Emp Login"),
            Pair(R.drawable.ic_intern, "Trainee Login"),
            Pair(R.drawable.ic_nav_rate_us, "Rate Us"),
            Pair(R.drawable.ic_nav_share, "Share")
        )

        // Add each custom item to the NavigationView
        val menuParent =
            navigationView.getHeaderView(0).findViewById<LinearLayout>(R.id.menu_container)

        menuItems.forEach { (iconRes, title) ->
            val customView = layoutInflater.inflate(R.layout.item_nav_menu, menuParent, false)

            val iconView = customView.findViewById<ImageView>(R.id.icon)
            val titleView = customView.findViewById<TextView>(R.id.title)

            // Set icon and title
            iconView.setImageResource(iconRes)
            titleView.text = title

            // Add click listener if needed
            customView.setOnClickListener {
                when (title) {
                    "Admin Login" -> {
                        // Start AdminLogin activity
                        val intent = Intent(this, TraineeAdminLogin::class.java)
                        startActivity(intent)
                    }

                    "Client Login" -> {
                        val intent = Intent(this, ClientLogin::class.java)
                        startActivity(intent)
                    }

                    "Intern/Emp Login" -> {
                        // Start InternLogin activity
                        val intent = Intent(this, InternLogin::class.java)
                        startActivity(intent)
                    }

                    "Trainee Login" -> {
                        // Start InternLogin activity
                        val intent = Intent(this, TraineeLogin::class.java)
                        startActivity(intent)
                    }

                    "Rate Us" -> {
                        // Open Play Store to rate the app
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$packageName")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            // In case the Play Store app is not installed, open it in a browser
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            )
                            startActivity(intent)
                        }
                    }

                    "Share" -> {
                        // Share the predefined message
                        val message =
                            "Makes360 - Your IT Partner\n\nSince 2018, Makes360 has delivered 114+ projects across 12+ industries. We specialize in brand building, marketing, and business consulting with lifetime free maintenance and 24/7 support. Let’s drive your digital success!\n\nDownload our app from Google Play Store. Click the link below:\n" +
                                    "https://play.google.com/store/apps/details?id=$packageName"

                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, message)

                        startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }

            // Add the custom view to the container
            menuParent.addView(customView)
        }
    }

    private fun setUpViews() {
        setUpImageSlider()
        setUpRecyclerView()
        setUpCompanyProfileList()
        setUpCompanyBrochure()
        setUpInternLoginBtn()
        setUpClientLoginBtn()
        setUpTraineeLoginForCGC()
        setUpFooter()
    }

    private fun setUpCompanyBrochure() {
        binding.companyBrochureCardView.setOnClickListener {
            val url =
                "https://www.makes360.com/application/makes360/files/Company-Profile-Makes360.pdf"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        }
    }

    private fun setUpTraineeLoginForCGC() {
        binding.traineeLoginCardViewForCGC.setOnClickListener {
            val intent = Intent(this, TraineeLogin::class.java)
            startActivity(intent)
        }
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
        binding.homeFooterTextView.text = getString(R.string.footer_text, currentYear)
    }

    private fun setUpCompanyProfileList() {
        companyProfileList.add(
            CompanyProfileData(
                "Services",
                R.drawable.ic_service,
                "https://www.makes360.com/services/"
            )
        )
        companyProfileList.add(
            CompanyProfileData(
                "Our Work",
                R.drawable.ic_our_work,
                "https://www.makes360.com/portfolio/"
            )
        )
        companyProfileList.add(
            CompanyProfileData(
                "Testimonial",
                R.drawable.ic_rating,
                "https://www.makes360.com/testimonials/"
            )
        )
        companyProfileList.add(
            CompanyProfileData(
                "About Us",
                R.drawable.ic_about_us,
                "https://www.makes360.com/about/"
            )
        )
        companyProfileList.add(
            CompanyProfileData(
                "Internship",
                R.drawable.ic_internship,
                "https://www.makes360.com/internship/"
            )
        )
        companyProfileList.add(
            CompanyProfileData(
                "Training",
                R.drawable.ic_training,
                "https://www.makes360.com/training"
            )
        )
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

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
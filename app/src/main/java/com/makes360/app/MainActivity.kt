package com.makes360.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.makes360.app.adapters.CompanyProfileAdapter
import com.makes360.app.databinding.ActivityMainBinding
import com.makes360.app.models.CompanyProfileData
import com.makes360.app.ui.client.ClientLogin
import com.makes360.app.ui.intern.InternLogin
import com.makes360.app.ui.trainee.TraineeAdminLogin
import com.makes360.app.ui.trainee.TraineeDashboard
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

        binding.swipeRefreshLayout.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                setUpViews()
            } else {
                showNoInternet()
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setUpViews() {
        setUpImageSlider()
        setUpRecyclerView()
        setUpCompanyProfileList()
        setUpCompanyBrochure()
        setUpInternLoginBtn()
        setUpClientLoginBtn()
        setUpTraineeLogin()
        setUpFooter()
        setUpNotification()
    }

    private fun setUpNotification() {
        // Check if the permission has been requested before
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isPermissionRequested = sharedPreferences.getBoolean("isPermissionRequested", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPermissionRequested) {
            // Request notification permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )

            // Mark as requested
            sharedPreferences.edit().putBoolean("isPermissionRequested", true).apply()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "You may miss important announcements. Please enable notifications from Settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setUpCompanyBrochure() {
        binding.companyBrochureCardView.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Are you sure to download company brochure?")

            // Set up the buttons
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss() // Close the dialog
                openUrl()
            }

            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Close the dialog without doing anything
            }

            // Display the dialog
            val dialog = builder.create()
            dialog.show()

            // Apply rounded corners and background
            dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)

            // Set button colors
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.material_flat_red))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.material_flat_green_dark))

            // Set title and message text color to black
            val textViewMessage = dialog.findViewById<TextView>(android.R.id.message)
            textViewMessage?.setTextColor(Color.BLACK)
        }
    }


    private fun openUrl() {
        val url =
            "https://www.makes360.com/application/makes360/files/Company-Profile-Makes360.pdf"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        startActivity(intent)
    }

    private fun setUpTraineeLogin() {
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
        companyProfileList.clear()

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
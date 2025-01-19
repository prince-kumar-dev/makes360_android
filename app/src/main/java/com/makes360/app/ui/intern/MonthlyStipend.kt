package com.makes360.app.ui.intern

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.makes360.app.BaseActivity
import com.makes360.app.databinding.ActivityMonthlyStipendBinding
import com.makes360.app.models.MonthlyStipendData
import com.makes360.app.R
import com.makes360.app.adapters.MonthlyStipendListAdapter
import com.makes360.app.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class MonthlyStipend : BaseActivity() {

    private var monthlyStipendParentList = mutableListOf<MonthlyStipendData>()
    private lateinit var mBinding: ActivityMonthlyStipendBinding
    private lateinit var adapter: MonthlyStipendListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {

        // Initialize binding
        mBinding = ActivityMonthlyStipendBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")
        if (email != null) {
            showLoader()
            setUpViews(email)
        } else {
            showToast("Invalid email! Please try again.")
        }

    }

    private fun showLoader() {
        mBinding.progressOverlay.visibility = View.VISIBLE
        mBinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        mBinding.progressOverlay.visibility = View.GONE
        mBinding.progressBar.visibility = View.GONE
    }

    private fun setUpViews(email: String) {
        fetchMonthlyStipend(email)
        setUpBackButton()
        setUpQuotes()
        setUpDayWiseMessage()
    }

    private fun setUpUnderDraftAndRaiseComplaint(
        details: List<MonthlyStipendDetails>,
        email: String
    ) {

        val monthsMap = mapOf(
            "1" to "Jan",
            "2" to "Feb",
            "3" to "Mar",
            "4" to "Apr",
            "5" to "May",
            "6" to "Jun",
            "7" to "Jul",
            "8" to "Aug",
            "9" to "Sep",
            "10" to "Oct",
            "11" to "Nov",
            "12" to "Dec"
        )


        val calendar = Calendar.getInstance()
        //val currentDay = calendar.get(Calendar.DAY_OF_MONTH) // Get current day of the month
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        // Get the last day of the current month dynamically
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val unpaidDetail = details.find { it.paymentStatus.trim() == "0" }
        val month = unpaidDetail?.month
        val year = unpaidDetail?.year

        // Check if the current day is between 21 and the last day of the month
        if (currentDay in 21..lastDayOfMonth && unpaidDetail != null) {

            mBinding.underDraftCardView.visibility = View.VISIBLE
            mBinding.viewDraftStipend.text = "Draft Ready (${monthsMap[month]} $year)"

            mBinding.underDraftCardView.setOnClickListener {
                val intent = Intent(this, Stipend::class.java)
                intent.putExtra("EMAIL", email)
                intent.putExtra("MONTH", month)
                intent.putExtra("YEAR", year)
                startActivity(intent)
            }

            if (currentDay <= 25) {
                mBinding.raiseComplaintCardView.visibility = View.VISIBLE
                // Raise Complaint Message SetUp
                mBinding.raiseComplaintCardView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val data =
                        Uri.parse("mailto:hr@makes360.com?subject=Complaint Regarding Salary/Stipend Slip&body=")
                    intent.data = data
                    startActivity(intent)
                }
            }

        } else {
            mBinding.underDraftCardView.visibility = View.GONE
            mBinding.raiseComplaintCardView.visibility = View.GONE
        }
    }

    private fun setUpDayWiseMessage() {

        // Messages mapped by phases (Motivation, Workload, Crying, Excited)
        val baseMessages = listOf(
            // Motivated Phase 1 to 7
            "🎉 Happy Happy Happy! My whole month's hard work, finally paid off! Let's get this month started strong! 💪",
            "💼 This month, I’ll impress my buddy and aim for that salary hike! Let's go! 🔥",
            "💡 Every small effort counts! Focused and smashing my goals! 📈",
            "📚 Knowledge is power, and I’m leveling up every day! 🚀",
            "💯 Consistency is key! Progress is showing! 💪",
            "🌱 Growing, evolving, and thriving. The journey is beautiful! 🌟",
            "🛠️ Let’s get things done! One task at a time, full focus mode ON! 💼",

            // Less Motivated Phase 8 to 14
            "😴 Feeling a little low today… But hey, gotta keep moving forward! 😌",
            "☕ Coffee, focus, repeat. It's one of those days… 😪",
            "🥱 10 days in… and I already feel like it’s been a year. But we move! 💼",
            "📊 Deadlines, meetings, emails… Why is this month so long? 😵‍💫",
            "😓 Motivation is slipping… but my bills won’t pay themselves! 🫠",
            "😴 Just wanna sleep, but this code won't write itself. 💻",
            "🧠 Brain is fried… Can I just skip to salary day already? 😵",

            // Workload Phase 15 to 20
            "🫠 Half the month gone, but the workload just doubled. SOS! 🆘",
            "🤯 Deadlines are attacking me from all directions. Someone send help! 📅",
            "💥 Meetings, emails, reports, repeat… Where’s the pause button? ⏸️",
            "😩 My to-do list is scarier than my bank balance. 😭",
            "⚠️ Warning: Workload level critical. Proceed with caution. 🚧",
            "🛑 5 minutes of peace, that's all I ask for. Just 5 minutes… 😫",

            // Crying & Waiting Phase 21 to 25
            "🥲 I just checked my bank account… it's a empty place. Heartbreak level: Maximum. 💔",
            "😭 Every notification sound gives me hope, but it's never the salary message. 📲",
            "🥹 Can someone remind my company that salary day exists? Please? 🫠",
            "😔 One week to go… I can almost hear my salary calling me. 💸",
            "😭 Wallet: Empty. Heart: Heavy. Dreams: Shattered. 🥀",

            // Excited Phase 26 to 30
            "⚡ The countdown begins… I can smell the salary vibes already! 🎉",
            "💰 Just a few more days! Hang in there, wallet! Rescue is near! 🛟",
            "🥳 One more sleep, and then… CHA-CHING! 🎊",
            "💥 It’s almost here! My bank account is preparing for impact. 🚀",
            "🎉 SALARY DAY IS HERE! 🤑 Time to pay bills, buy happiness, and save… maybe. 😅"
        )

        // Get the current day, month, and year
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Months are 0-indexed
        val currentYear = calendar.get(Calendar.YEAR)

        // Function to check leap year
        fun isLeapYear(year: Int): Boolean {
            return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        }

        // Determine the number of days in the current month
        val daysInMonth = when (currentMonth) {
            1, 3, 5, 7, 8, 10, 12 -> 31 // Months with 31 days
            4, 6, 9, 11 -> 30           // Months with 30 days
            2 -> if (isLeapYear(currentYear)) 29 else 28 // February in leap and non-leap year
            else -> 30 // Fallback (should not occur)
        }

        // Dynamically select a message
        val messageOfTheDay = if (currentDay <= daysInMonth) {
            baseMessages[(currentDay - 1).coerceAtMost(baseMessages.size - 1)]
        } else {
            "🎯 It's a special day! Keep shining bright! ✨"
        }

        // Display the message in the TextView
        mBinding.monthDayMessageTV.text = messageOfTheDay

    }

    private fun setUpQuotes() {
        // Set up background color of quoteCardView

        // Gradient List with Light/Dark Mapping
        val gradientMap = mapOf(
            R.drawable.gradient_aqua to "DARK",
            R.drawable.gradient_purple to "DARK",
            R.drawable.gradient_emerald to "LIGHT",
            R.drawable.gradient_cosmic to "DARK",
            R.drawable.gradient_sky to "LIGHT",
            R.drawable.gradient_nightfall to "DARK",
            R.drawable.gradient_mint to "LIGHT",
        )

        val randomGradient = gradientMap.keys.random()

        mBinding.quoteCard.setBackgroundResource(randomGradient)

        // Set Text Color Based on Gradient Type
        when (gradientMap[randomGradient]) {
            "DARK" -> {
                mBinding.quoteText.setTextColor(Color.WHITE)
                mBinding.quoteSymbolTop.setTextColor(Color.WHITE)
                mBinding.quoteSymbolBottom.setTextColor(Color.WHITE)
                mBinding.quoteCard.setCardBackgroundColor(Color.parseColor("#60000000")) // Semi-transparent Dark
            }

            "LIGHT" -> {
                mBinding.quoteText.setTextColor(Color.BLACK)
                mBinding.quoteSymbolTop.setTextColor(Color.BLACK)
                mBinding.quoteSymbolBottom.setTextColor(Color.BLACK)
                mBinding.quoteCard.setCardBackgroundColor(Color.parseColor("#80FFFFFF")) // Semi-transparent Light
            }
        }

        // Fetch quote from the server
        val queue = Volley.newRequestQueue(this)
        val url = "https://www.makes360.com/application/makes360/internship/quotes.php"
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                mBinding.quoteText.text = response.replace("\"", "")
            },
            { error ->
                mBinding.quoteText.text = "Failed to load quote"
            })

        queue.add(stringRequest)
        mBinding.quoteText.startAnimation(
            AnimationUtils.loadAnimation(
                this,
                R.anim.quote_card_anim
            )
        )
    }

    private fun setUpBackButton() {
        mBinding.backImageView.setOnClickListener {
            finish()
        }
    }

    private fun fetchMonthlyStipend(email: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/internship/monthly-stipend.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                // Write email to request body
                val requestBody = "email=$email"
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()
                outputStream.close()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()

                    runOnUiThread {
                        hideLoader()
                    }

                    if (response.isNotEmpty()) {
                        val jsonResponse = try {
                            JSONArray(response)
                        } catch (e: JSONException) {
                            // Handle non-array responses
                            JSONObject(response)
                        }

                        if (jsonResponse is JSONObject) {
                            // Handle error or message responses
                            val errorMessage = jsonResponse.optString("error", "")
                            val message = jsonResponse.optString("message", "")

                            runOnUiThread {
                                if (errorMessage.isNotEmpty() || message == "No records found for the given email.") {
                                    showToast(errorMessage.ifEmpty { "No Record Found!" })
                                    noRecordCardView()
                                } else {
                                    showToast("Unexpected server response.")
                                }
                            }
                        } else if (jsonResponse is JSONArray && jsonResponse.length() > 0) {
                            // Valid stipend data
                            val monthlyStipendDetails = parseResponse(response)
                            runOnUiThread {
                                setUpMonthlyStipendParentList(monthlyStipendDetails, email)
                                setUpUnderDraftAndRaiseComplaint(monthlyStipendDetails, email)
                                setUpRecyclerView()
                            }
                        } else {
                            runOnUiThread {
                                showToast("No Record Found")
                                noRecordCardView()
                            }
                        }
                    } else {
                        runOnUiThread {
                            showToast("Empty response from server.")
                            noRecordCardView()
                        }
                    }
                } else {
                    runOnUiThread {
                        hideLoader()
                        showToast("Failed to fetch stipend details. Please try again.")
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                runOnUiThread {
                    hideLoader()
                    showToast("Invalid data format from server.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    hideLoader()
                    showToast("An error occurred: ${e.message}")
                }
            }
        }
    }


    private fun noRecordCardView() {
        mBinding.noRecordCardView.visibility = View.VISIBLE
    }

    private fun parseResponse(response: String): List<MonthlyStipendDetails> {
        val jsonResponse = JSONArray(response)
        val monthlyStipendDetailsList = mutableListOf<MonthlyStipendDetails>()

        for (i in 0 until jsonResponse.length()) {
            val record = jsonResponse.getJSONObject(i)
            monthlyStipendDetailsList.add(
                MonthlyStipendDetails(
                    month = record.optString("month", "Unknown"),
                    year = record.optString("year", "Unknown"),
                    paymentStatus = record.optString("payment_status", "Unknown")
                )
            )
        }

        return monthlyStipendDetailsList
    }

    private fun setUpRecyclerView() {
        adapter = MonthlyStipendListAdapter(this, monthlyStipendParentList)
        mBinding.monthlyStipendRV.layoutManager = LinearLayoutManager(this)
        mBinding.monthlyStipendRV.adapter = adapter

    }

    private fun setUpMonthlyStipendParentList(details: List<MonthlyStipendDetails>, email: String) {

        val monthsMap = mapOf(
            "1" to "January",
            "2" to "February",
            "3" to "March",
            "4" to "April",
            "5" to "May",
            "6" to "June",
            "7" to "July",
            "8" to "August",
            "9" to "September",
            "10" to "October",
            "11" to "November",
            "12" to "December"
        )

        for (i in details.indices.reversed()) {
            if (details[i].paymentStatus == "1")
                monthlyStipendParentList.add(
                    MonthlyStipendData(
                        title = monthsMap[details[i].month] + ", " + details[i].year,
                        logo = R.drawable.ic_calendar,
                        idMonth = details[i].month,
                        idYear = details[i].year,
                        email = email
                    )
                )
        }

    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class MonthlyStipendDetails(
        val month: String,
        val year: String,
        val paymentStatus: String
    )
}
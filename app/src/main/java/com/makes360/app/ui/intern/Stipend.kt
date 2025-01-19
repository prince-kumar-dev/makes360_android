package com.makes360.app.ui.intern

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.makes360.app.BaseActivity
import com.makes360.app.databinding.ActivityStipendBinding
import com.makes360.app.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class Stipend : BaseActivity() {

    private lateinit var mBinding: ActivityStipendBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        mBinding = ActivityStipendBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val email = intent.getStringExtra("EMAIL")
        val month = intent.getStringExtra("MONTH")
        val year = intent.getStringExtra("YEAR")

        if (email != null && month != null && year != null) {
            showLoader()
            setUpViews(email, month, year)
        } else {
            showToast("Invalid email! or id! Please try again.")
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

    private fun setUpViews(email: String, month: String, year: String) {
        fetchStipendDetails(email, month, year)
        setUpBackButton()
    }

    private fun setUpBackButton() {
        mBinding.backImageView.setOnClickListener {
            finish()
        }
    }

    private fun setUpToolbar(month: String?, year: String?) {
        setSupportActionBar(mBinding.stipendToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mBinding.titleToolbarTxt.text = "$month, $year"
        mBinding.stipendToolbar.setTitleTextColor(Color.WHITE)
    }

    private fun fetchStipendDetails(email: String, month: String, year: String) {
//        if (!checkInternetConnection()) {
//            hideLoader()
//            showToast("No internet connection. Please check your connection.")
//            return
//        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/internship/stipend-details.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                // Write email to request body
                val requestBody = "email=$email&month=$month&year=$year"
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()
                outputStream.close()

                // Check response code
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val stipendDetails = parseResponse(response)

                    // Update UI on the main thread
                    runOnUiThread {
                        hideLoader()
                        setUpStipendSlip(stipendDetails)
                    }
                } else {
                    runOnUiThread {
                        hideLoader()
                        showToast("Failed to fetch profile details. Please try again.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    hideLoader()
                    showToast("Error occurred: ${e.message}")
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setUpStipendSlip(details: List<StipendDetails>) {
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

        mBinding.stipendSlip.text =
            "Stipend Details - " + monthsMap[details[0].month] + ", " + details[0].year
        mBinding.internshipId.text = details[0].certificateNumber
        mBinding.jobRole.text = details[0].jobRole
        mBinding.currentStipend.text = "₹" + details[0].currentStipend
        mBinding.logHour.text = details[0].logHour + " Hrs"
        mBinding.paidLeave.text = details[0].paidLeave + " Days"
        mBinding.unpaidLeave.text = details[0].unpaidLeave + " Days"
        mBinding.workFromHome.text = details[0].workFromHome + " Days"
        mBinding.deadlinesMissedCost.text = "₹" + details[0].deadlinesMissed
        mBinding.deadlinesMissedRemarks.text = details[0].deadlinesMissedRemarks
        mBinding.advancePayment.text = "₹" + details[0].advancedPayment
        mBinding.advancePaymentRemarks.text = details[0].advancedPaymentRemarks
        mBinding.bonus.text = "₹" + details[0].bonus
        mBinding.bonusRemarks.text = details[0].bonusRemarks
        mBinding.totalStipend.text = "₹" + details[0].paidAmount
        mBinding.paymentStatus.text = if (details[0].paymentStatus == "1") "Paid" else "Processing"


        if (details[0].paymentStatus == "1") {
            val originalDate = details[0].paymentDate // "YYYY-MM-DD" format
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault())

            // Parse and format the date
            val parsedDate = inputFormat.parse(originalDate)
            val formattedDate = parsedDate?.let { outputFormat.format(it) }
            // Assign formatted date to the TextView
            mBinding.paymentDate.text = formattedDate ?: originalDate
            mBinding.paymentMethod.text = details[0].paymentMethod
        } else {
            mBinding.paymentDate.text = "Processing"
            mBinding.paymentMethod.text = "Processing"
        }


        mBinding.bankAcNo.text = details[0].bankAcNo
        mBinding.ifscCode.text = details[0].ifsc
        mBinding.notes.text = details[0].notes

        setUpToolbar(monthsMap[details[0].month], details[0].year)
        if (details[0].fullTime == "1") {
            fetchEmployeeDisclaimer()
        } else {
            fetchInternDisclaimer()
        }
    }

    private fun fetchInternDisclaimer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/internship/files/intern-disclaimer.txt")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val text = connection.inputStream.bufferedReader().use { it.readText() }
                withContext(Dispatchers.Main) {
                    mBinding.disclaimer.text = text
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mBinding.disclaimer.text = "Failed to load disclaimer: ${e.message}"
                }
            }
        }
    }

    private fun fetchEmployeeDisclaimer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/internship/files/employee-disclaimer.txt")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val text = connection.inputStream.bufferedReader().use { it.readText() }
                withContext(Dispatchers.Main) {
                    mBinding.disclaimer.text = text
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mBinding.disclaimer.text = "Failed to load disclaimer: ${e.message}"
                }
            }
        }
    }

    private fun parseResponse(response: String): List<StipendDetails> {
        val jsonResponse = JSONArray(response)
        val stipendDetailsList = mutableListOf<StipendDetails>()

        for (i in 0 until jsonResponse.length()) {
            val record = jsonResponse.getJSONObject(i)
            stipendDetailsList.add(
                StipendDetails(
                    certificateNumber = record.optString("certificate_number", "Unknown"),
                    month = record.optString("month", "Unknown"),
                    year = record.optString("year", "Unknown"),
                    jobRole = record.optString("job_role", "Unknown"),
                    bankAcNo = record.optString("bank_ac_number", "Unknown"),
                    ifsc = record.optString("ifsc", "Unknown"),
                    fullTime = record.optString("full_time", "Unknown"),
                    baseStipend = record.optString("base_stipend", "Unknown"),
                    currentStipend = record.optString("current_stipend", "Unknown"),
                    workFromHome = record.optString("work_from_home", "Unknown"),
                    paidLeave = record.optString("paid_leave", "Unknown"),
                    unpaidLeave = record.optString("unpaid_leave", "Unknown"),
                    logHour = record.optString("log_hour", "Unknown"),
                    deadlinesMissed = record.optString("deadlines_missed", "Unknown"),
                    deadlinesMissedRemarks = record.optString(
                        "deadlines_missed_remarks",
                        "Unknown"
                    ),
                    advancedPayment = record.optString("advanced_payment", "Unknown"),
                    advancedPaymentRemarks = record.optString(
                        "advanced_payment_remarks",
                        "Unknown"
                    ),
                    bonus = record.optString("bonus", "Unknown"),
                    bonusRemarks = record.optString("bonus_remarks", "Unknown"),
                    paymentDate = record.optString("payment_date", "Unknown"),
                    paymentStatus = record.optString("payment_status", "Unknown"),
                    paidAmount = record.optString("paid_amount", "Unknown"),
                    paymentMethod = record.optString("payment_method", "Unknown"),
                    paymentReceiptUrl = record.optString("payment_receipt_url", "Unknown"),
                    notes = record.optString("notes", "Unknown"),
                    paymentUpdated = record.optString("payment_updated", "Unknown")
                )
            )
        }

        return stipendDetailsList
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    data class StipendDetails(
        val certificateNumber: String,
        val month: String,
        val year: String,
        val jobRole: String,
        val bankAcNo: String,
        val ifsc: String,
        val fullTime: String,
        val baseStipend: String,
        val currentStipend: String,
        val workFromHome: String,
        val paidLeave: String,
        val unpaidLeave: String,
        val logHour: String,
        val deadlinesMissed: String,
        val deadlinesMissedRemarks: String,
        val advancedPayment: String,
        val advancedPaymentRemarks: String,
        val bonus: String,
        val bonusRemarks: String,
        val paymentDate: String,
        val paymentStatus: String,
        val paidAmount: String,
        val paymentMethod: String,
        val paymentReceiptUrl: String,
        val notes: String,
        val paymentUpdated: String
    )
}
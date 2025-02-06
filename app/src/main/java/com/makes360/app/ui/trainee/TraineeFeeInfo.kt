package com.makes360.app.ui.trainee

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.makes360.app.BaseActivity
import com.makes360.app.R
import com.makes360.app.databinding.ActivityTraineeFeeInfoBinding
import com.makes360.app.databinding.ActivityTraineeProfileBinding
import com.makes360.app.ui.trainee.TraineeDashboard.TraineeDetails
import com.makes360.app.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class TraineeFeeInfo : BaseActivity() {

    private lateinit var mBinding: ActivityTraineeFeeInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NetworkUtils.isInternetAvailable(this)) {
            hideNoInternet()
            loadContent()
        }
    }

    private fun loadContent() {
        // Initialize binding
        mBinding = ActivityTraineeFeeInfoBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Get the email passed from the previous activity
        val email = intent.getStringExtra("EMAIL")

        if (email != null) {
            showLoader()
            fetchTraineeFeeInfo(email)
        } else {
            showToast("Invalid email! Please try again.")
        }
        setUpBackButton()
    }

    private fun fetchTraineeFeeInfo(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://www.makes360.com/application/makes360/trainee/trainee-profile-details.php")
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

                // Check response code
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val feeDetails = parseResponse(response)

                    // Update UI on the main thread
                    runOnUiThread {
                        hideLoader()
                        updateFeeInfoUI(feeDetails)
                    }
                } else {
                    runOnUiThread {
                        hideLoader()
                        showToast("Failed to fetch details. Please try again.")
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

    private fun updateFeeInfoUI(feeDetails: TraineeFeeInfo) {

        if(feeDetails.status == "Success") {
            mBinding.registrationFeeTextView.text = "Joined with Makes360"
            mBinding.registrationFeeCardView.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.material_core_green)
            )
            mBinding.registrationCard.strokeColor = ContextCompat.getColor(this, R.color.material_core_green)


            mBinding.amountFeeTextView.text = "₹ " + feeDetails.amount
            mBinding.txnIdFeeTextView.text = feeDetails.txnId

            val dateString = feeDetails.createdAt

            val formattedDate = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)!!
            )

            mBinding.registrationDateTextView.text = formattedDate

            // Attach the click listener to the info icon
            mBinding.registrationStatusImageView.setOnClickListener { view ->
                showPopup(view, "Registration fee has been successfully paid.")
            }
        }

        if(feeDetails.firstInst.isEmpty()) {
            mBinding.firstInstallmentFeeTextView.text = "Not Paid"
            mBinding.firstInstallmentFeeCardView.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.material_core_red)
            )
            mBinding.firstInstallmentStatusImageView.setOnClickListener { view ->
                showPopup(view, "First installment has not yet been paid.")
            }
            mBinding.payFeeCardView.setOnClickListener {
                val url = "https://www.makes360.com/training/final-year-internship/fee/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            mBinding.registrationCard.strokeColor = ContextCompat.getColor(this, R.color.material_core_red)
        } else {
            mBinding.payFeeCardView.visibility = View.GONE

            when (feeDetails.firstInstApproved) {
                "null" -> {
                    mBinding.firstInstallmentFeeCardView.setCardBackgroundColor(
                        ContextCompat.getColor(this, R.color.material_core_yellow)
                    )
                    mBinding.firstInstallmentFeeTextView.text = "Pending Approval"

                    mBinding.firstInstallmentStatusImageView.setOnClickListener { view ->
                        showPopup(
                            view,
                            "First installment has been paid. Our team will review and approve it shortly. Rest assured, there's nothing to worry about."
                        )
                    }
                    mBinding.registrationCard.strokeColor = ContextCompat.getColor(this, R.color.material_core_yellow)
                }
                "0" -> {
                    mBinding.firstInstallmentFeeTextView.text = "Payment Rejected"
                    mBinding.firstInstallmentFeeCardView.setCardBackgroundColor(
                        ContextCompat.getColor(this, R.color.material_core_red)
                    )
                    mBinding.firstInstallmentStatusImageView.setOnClickListener { view ->
                        showPopup(
                            view,
                            "Your payment has not been approved. Please reach out to support for assistance."
                        )
                    }
                    mBinding.registrationCard.strokeColor = ContextCompat.getColor(this, R.color.material_core_red)
                }
                "1" -> {
                    mBinding.firstInstallmentFeeCardView.setCardBackgroundColor(
                        ContextCompat.getColor(this, R.color.material_core_green)
                    )
                    mBinding.firstInstallmentFeeTextView.text = "Successfully Paid"

                    mBinding.firstInstallmentStatusImageView.setOnClickListener { view ->
                        showPopup(view, "First installment has been paid and successfully verified.")
                    }
                    mBinding.registrationCard.strokeColor = ContextCompat.getColor(this, R.color.material_core_green)
                }
            }
        }
    }

    private fun showPopup(view: View, message: String) {
        // Create a Dialog instance
        val dialog = Dialog(view.context)

        // Inflate the custom popup layout
        val popupView = LayoutInflater.from(view.context).inflate(R.layout.popup_message_box, null)
        dialog.setContentView(popupView)

        // Set the message in the TextView
        val messageTextView = popupView.findViewById<TextView>(R.id.messageTextView)
        messageTextView.text = message

        // Set dialog properties to center it and blur the background
        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Transparent background
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) // Dims the background
            setDimAmount(0.5f) // Adjust dim intensity (0.0f - no dim, 1.0f - full dim)
        }

        // Show the dialog
        dialog.show()
    }

    private fun setUpBackButton() {
        findViewById<ImageView>(R.id.backImageView).setOnClickListener {
            finish()
        }
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

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun parseResponse(response: String): TraineeFeeInfo {
        val jsonResponse = JSONObject(response)
        return TraineeFeeInfo(
            name = jsonResponse.optString("name", "Unknown"),
            firstInst = jsonResponse.optString("first_inst", "Unknown"),
            firstInstApproved = jsonResponse.optString("first_approved", "Unknown"),
            offerLetter = jsonResponse.optString("offer_letter", "Unknown"),
            motherName = jsonResponse.optString("mother_name", "Unknown"),
            email = jsonResponse.optString("email", "Unknown"),
            phone = jsonResponse.optString("phone", "Unknown"),
            college = jsonResponse.optString("college", "Unknown"),
            department = jsonResponse.optString("department", "Unknown"),
            rollNo = jsonResponse.optString("roll_no", "Unknown"),
            dob = jsonResponse.optString("dob", "Unknown"),
            gender = jsonResponse.optString("gender", "Unknown"),
            address = jsonResponse.optString("address", "Unknown"),
            status = jsonResponse.optString("status", "Unknown"),
            amount = jsonResponse.optString("amount", "Unknown"),
            txnId = jsonResponse.optString("txnId", "Unknown"),
            createdAt = jsonResponse.optString("created_at", "Unknown"),
            updated = jsonResponse.optString("updated", "Unknown"),
            lastLogin = jsonResponse.optString("lastLogin", "Unknown")
        )
    }

    data class TraineeFeeInfo (
        val name: String,
        val firstInst: String,
        val firstInstApproved: String,
        val offerLetter: String,
        val motherName: String,
        val email: String,
        val phone: String,
        val college: String,
        val department: String,
        val rollNo: String,
        val dob: String,
        val gender: String,
        val address: String,
        val status: String,
        val amount: String,
        val txnId: String,
        val createdAt: String,
        val updated: String,
        val lastLogin: String
    )
}
package com.makes360.android.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.makes360.android.R
import com.makes360.android.models.InternDetailsRV
import com.makes360.android.ui.intern.InternProfile
import com.makes360.android.ui.intern.MonthlyStipend

class InternDetailsAdapter(
    private var context: Context,
    private val internDetailsList: List<InternDetailsRV>
) : RecyclerView.Adapter<InternDetailsAdapter.InternDetailsViewHolder>() {

    inner class InternDetailsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.detailsIcon)
        val title: TextView = itemView.findViewById(R.id.detailsTitle)
        val cardViewContainer: CardView = itemView.findViewById(R.id.detailsCardContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InternDetailsViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_intern_details, parent, false)
        return InternDetailsViewHolder(view)
    }

    override fun getItemCount(): Int = internDetailsList.size

    override fun onBindViewHolder(holder: InternDetailsViewHolder, position: Int) {
        val details = internDetailsList[position]
        holder.icon.setImageResource(details.icon)
        holder.title.text = details.title

        // Update card appearance based on conditions
        if ((details.title == "Resume" && details.resumeLink.isNullOrEmpty() && details.applicationStatus == 1) ||
            (details.title == "Video Resume" && details.videoResumeLink.isNullOrEmpty() && details.applicationStatus == 1)
        ) {
            highlightCard(holder)
        }

        holder.itemView.setOnClickListener {
            when (details.title) {
                "Profile Details" -> handleProfile(details)
                "Offer Letter" -> handleOfferLetter(details)
                "Resume" -> handleResume(details, holder)
                "Video Resume" -> handleVideoResume(details, holder)
                "Stipend" -> handleStipend(details)
                "Certificate" -> handleCertificate(details)
                else -> {
                    Toast.makeText(context, "No valid action available!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // --- Profile Handling ---
    private fun handleProfile(details: InternDetailsRV) {
        val intent = Intent(context, InternProfile::class.java).apply {
            putExtra("EMAIL", details.email)
        }
        context.startActivity(intent)
    }

    // --- Offer Letter Handling ---
    private fun handleOfferLetter(details: InternDetailsRV) {
        if (!details.offerLetterLink.isNullOrEmpty()) {
            openUrl(details.offerLetterLink)
        } else {
            Toast.makeText(
                context,
                "Offer letter is currently not issued!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --- Resume Handling ---
    private fun handleResume(details: InternDetailsRV, holder: InternDetailsViewHolder) {
        when {
            !details.resumeLink.isNullOrEmpty() -> openUrl(details.resumeLink)
            details.resumeLink.isNullOrEmpty() && details.applicationStatus == 1 -> {
                highlightCard(holder)
                openUrl("https://www.makes360.com/internship/apply/profile")
            }

            else -> Toast.makeText(
                context,
                "Currently you are not eligible to upload resume",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --- Video Resume Handling ---
    private fun handleVideoResume(details: InternDetailsRV, holder: InternDetailsViewHolder) {
        when {
            !details.videoResumeLink.isNullOrEmpty() -> openUrl(details.videoResumeLink)
            details.videoResumeLink.isNullOrEmpty() && details.applicationStatus == 1 -> {
                highlightCard(holder)
                openUrl("https://www.makes360.com/internship/apply/profile")
            }

            else -> Toast.makeText(
                context,
                "Currently you are not eligible to upload Video Resume",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --- Stipend Handling ---
    private fun handleStipend(details: InternDetailsRV) {
        val intent = Intent(context, MonthlyStipend::class.java).apply {
            putExtra("EMAIL", details.email)
        }
        context.startActivity(intent)
    }

    // --- Certificate Handling ---
    private fun handleCertificate(details: InternDetailsRV) {
        if (details.title == "Certificate") {
            when (details.certificateLink) {
                "1" -> {
                    // Certificate allotted, open the URL
                    openUrl("https://www.makes360.com/certificate/internship/")
                }
                "0" -> {
                    // Certificate explicitly marked as not allotted
                    Toast.makeText(
                        context,
                        "Certificate is currently not issued!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    // Default case for null or unexpected values
                    Toast.makeText(
                        context,
                        "Invalid or missing certificate details. Please check back later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            // Fallback if the title is not "Certificate"
            Toast.makeText(
                context,
                "No valid action available!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --- Highlight Card for Attention ---
    private fun highlightCard(holder: InternDetailsViewHolder) {
        holder.cardViewContainer.setCardBackgroundColor(
            ContextCompat.getColor(context, R.color.material_flat_red)
        )
        holder.title.setTextColor(ContextCompat.getColor(context, R.color.white))
    }

    // --- Open URL in Browser ---
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(
                context,
                "No application available to open the URL.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
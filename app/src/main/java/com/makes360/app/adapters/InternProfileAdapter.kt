package com.makes360.app.adapters

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.InternProfileCategory

class InternProfileAdapter(
    private val context: Context,
    private val categories: List<InternProfileCategory>
) : RecyclerView.Adapter<InternProfileAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.category_name)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrow_icon)
        val expandableContent: LinearLayout = itemView.findViewById(R.id.expandable_content)
        val categoryIcon: ImageView = itemView.findViewById(R.id.category_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_category, parent, false)
        return CategoryViewHolder(view)
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.categoryName.text = category.title
        holder.expandableContent.removeAllViews()

        for ((key, value) in category.details) {
            val detailLayout = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_profile_detail, null) as LinearLayout

            val detailNameTextView = detailLayout.findViewById<TextView>(R.id.detailName)
            val detailValueTextView = detailLayout.findViewById<TextView>(R.id.detailValue)
            val cardViewBtn = detailLayout.findViewById<CardView>(R.id.cardViewButton)
            val cardViewTxt = detailLayout.findViewById<TextView>(R.id.cardViewButtonTxtView)

            when (key) {
                "Duration:" -> {
                    if (category.applicationStatus >= 5) {

                        cardViewBtn.visibility = View.VISIBLE
                        cardViewTxt.text = "Request Extension"

                        val phoneNumber = category.adminMobileNo
                        val message = """
                        Hi,
                        
                        I'm ${category.name}, Intern ID: ${category.internID}.
                        
                        I'm writing to request an extension of my internship duration. 
                        I'm highly interested in continuing my work and believe I can contribute significantly in the extended period.
                        
                        Could I please discuss the possibility of extending my internship to [Desired Duration]?
                        
                        Thank you for your time and consideration.
                        
                        Sincerely,
                        ${category.name}
                    """.trimIndent()

                        cardViewBtn.setOnClickListener {
                            if (phoneNumber.isNotEmpty()) {
                                val encodedMessage = Uri.encode(message)
                                val whatsappUrl = "https://wa.me/$phoneNumber?text=$encodedMessage"

                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        "No app found to handle this request",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Phone number is invalid.",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }
                }

                "Completion Date:" -> {
                    if (category.applicationStatus == 6) {
                        cardViewBtn.visibility = View.VISIBLE

                        val url = if (!category.videoTestimonialLink.isNullOrEmpty()) {
                            cardViewTxt.text = "Video Testimonial"
                            "https://www.makes360.com${category.videoTestimonialLink}"
                        } else {
                            cardViewTxt.text = "Upload Video Testimonial"
                            "https://www.makes360.com/internship/apply/upload-testimonials"
                        }

                        cardViewBtn.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                }
            }

            detailNameTextView.text = key
            detailValueTextView.text = value
            holder.expandableContent.addView(detailLayout)
        }

        holder.expandableContent.visibility = if (category.isExpanded) View.VISIBLE else View.GONE
        holder.arrowIcon.setImageResource(
            if (category.isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow
        )

        holder.itemView.findViewById<LinearLayout>(R.id.header_layout).setOnClickListener {
            category.isExpanded = !category.isExpanded
            notifyItemChanged(position)
        }

        holder.categoryIcon.setImageResource(category.icon)
    }

    override fun getItemCount(): Int = categories.size
}
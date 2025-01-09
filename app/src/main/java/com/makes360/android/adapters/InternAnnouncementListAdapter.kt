package com.makes360.android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.android.R
import com.makes360.android.models.InternAnnouncementListRV
import java.text.SimpleDateFormat
import java.util.Locale

class InternAnnouncementListAdapter(
    private var announcements: List<InternAnnouncementListRV>
) : RecyclerView.Adapter<InternAnnouncementListAdapter.AnnouncementViewHolder>() {

    inner class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val announcementDateTxt: TextView = itemView.findViewById(R.id.announcementDateTxt)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrow_icon)
        val expandableContent: LinearLayout = itemView.findViewById(R.id.expandable_content)
    }

    fun setAnnouncements(newAnnouncements: List<InternAnnouncementListRV>) {
        this.announcements = newAnnouncements
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_intern_announcement_list_parent, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun getItemCount(): Int {
        return announcements.size
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val data = announcements[position]

        // Set date
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

        try {
            val date = inputDateFormat.parse(data.date)
            holder.announcementDateTxt.text = date?.let { outputDateFormat.format(it) } ?: "Invalid Date"
        } catch (e: Exception) {
            holder.announcementDateTxt.text = "Invalid Date"
        }

        holder.expandableContent.removeAllViews()

        // Inflate child layout for expandable content
        val detailLayout = LayoutInflater.from(holder.itemView.context)
            .inflate(R.layout.item_intern_announcement_list_child, null) as LinearLayout

        val announcementWebView = detailLayout.findViewById<WebView>(R.id.announcementWebView)

        // Configure WebView settings
        with(announcementWebView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            textZoom = 220
        }

        announcementWebView.setOnLongClickListener {
            // Do nothing on long press
            true
        }

        // Load content in WebView
        announcementWebView.loadDataWithBaseURL(null, data.message, "text/html", "UTF-8", null)

        // Add the WebView to the expandable content
        holder.expandableContent.addView(detailLayout)

        // Manage expand/collapse state
        holder.expandableContent.visibility = if (data.isExpanded) View.VISIBLE else View.GONE
        holder.arrowIcon.setImageResource(
            if (data.isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
        )

        // Toggle expand/collapse on header click
        holder.itemView.findViewById<LinearLayout>(R.id.header_layout).setOnClickListener {
            data.isExpanded = !data.isExpanded
            notifyItemChanged(position)
        }
    }
}
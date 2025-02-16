package com.makes360.app.adapters.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.client.ContactLogData
import java.text.SimpleDateFormat
import java.util.Locale

class ContactLogAdapter(
    private var contactLogs: List<ContactLogData>
) : RecyclerView.Adapter<ContactLogAdapter.ContactLogViewHolder>() {

    inner class ContactLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactDateTxt: TextView = itemView.findViewById(R.id.contactDateTxt)
        val expandableContent: LinearLayout = itemView.findViewById(R.id.expandable_content)
    }

    fun setContactLogs(newContactLogs: List<ContactLogData>) {
        this.contactLogs = newContactLogs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_log_parent, parent, false)
        return ContactLogViewHolder(view)
    }

    override fun getItemCount(): Int {
        return contactLogs.size
    }

    override fun onBindViewHolder(holder: ContactLogViewHolder, position: Int) {
        val data = contactLogs[position]

        // Set date
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault())

        try {
            val date = inputDateFormat.parse(data.date)
            holder.contactDateTxt.text = date?.let { outputDateFormat.format(it) } ?: "Invalid Date"
        } catch (e: Exception) {
            holder.contactDateTxt.text = "Invalid Date"
        }

        holder.expandableContent.removeAllViews()

        val detailLayout = LayoutInflater.from(holder.itemView.context)
            .inflate(R.layout.item_contact_log_child, null) as LinearLayout

        val contactLogWebView = detailLayout.findViewById<WebView>(R.id.contactLogWebView)

        // Configure WebView settings
        with(contactLogWebView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            textZoom = 220
        }

        contactLogWebView.setOnLongClickListener {
            // Do nothing on long press
            true
        }

        contactLogWebView.loadDataWithBaseURL(null, data.message, "text/html", "UTF-8", null)

        holder.expandableContent.addView(detailLayout)

    }
}
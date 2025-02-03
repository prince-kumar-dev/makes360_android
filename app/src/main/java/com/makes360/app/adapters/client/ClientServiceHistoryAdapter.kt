package com.makes360.app.adapters.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.client.ClientServiceHistoryData
import java.text.SimpleDateFormat
import java.util.Locale

class ClientServiceHistoryAdapter (
    private var serviceHistoryList: List<ClientServiceHistoryData>
): RecyclerView.Adapter<ClientServiceHistoryAdapter.ClientServiceHistoryViewHolder>() {

    inner class ClientServiceHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val expandableContent: LinearLayout = itemView.findViewById(R.id.expandable_content)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrow_icon)
        val serviceDate: TextView = itemView.findViewById(R.id.serviceDateTxt)
        val viewDetails: CardView = itemView.findViewById(R.id.viewDetailsCardView)
    }

    fun setServiceHistory(newServiceHistoryList: List<ClientServiceHistoryData>) {
        this.serviceHistoryList = newServiceHistoryList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClientServiceHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_history_parent, parent, false)
        return ClientServiceHistoryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return serviceHistoryList.size
    }

    override fun onBindViewHolder(holder: ClientServiceHistoryViewHolder, position: Int) {
        val serviceHistory = serviceHistoryList[position]

        // Set date
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

        try {
            val date = inputDateFormat.parse(serviceHistory.serviceDate)
            holder.serviceDate.text = date?.let { outputDateFormat.format(it) } ?: "Invalid Date"
        } catch (e: Exception) {
            holder.serviceDate.text = "Invalid Date"
        }

        holder.expandableContent.removeAllViews()

        val detailLayout = LayoutInflater.from(holder.itemView.context)
            .inflate(R.layout.item_service_history_child, null) as LinearLayout

        val serviceHistoryWebView = detailLayout.findViewById<WebView>(R.id.serviceHistoryWebView)

        // Configure WebView settings
        with(serviceHistoryWebView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            textZoom = 220
        }

        serviceHistoryWebView.setOnLongClickListener {
            // Do nothing on long press
            true
        }

        serviceHistoryWebView.loadDataWithBaseURL(null, serviceHistory.serviceDetails, "text/html", "UTF-8", null)

        holder.expandableContent.addView(detailLayout)

        holder.expandableContent.visibility = if (serviceHistory.isExpanded) View.VISIBLE else View.GONE
        holder.arrowIcon.setImageResource(
            if (serviceHistory.isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_right_arrow
        )

        holder.viewDetails.setOnClickListener {
            serviceHistory.isExpanded = !serviceHistory.isExpanded
            notifyItemChanged(position)
        }

    }
}
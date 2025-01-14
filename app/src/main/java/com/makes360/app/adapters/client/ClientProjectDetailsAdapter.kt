package com.makes360.app.adapters.client

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.client.ClientProjectDetailsData

class ClientProjectDetailsAdapter(
    private val context: Context,
    private val projectDetailsList: List<ClientProjectDetailsData>
) : RecyclerView.Adapter<ClientProjectDetailsAdapter.ClientProjectDetailsViewHolder>() {

    inner class ClientProjectDetailsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val expandableContent: LinearLayout = itemView.findViewById(R.id.expandable_content)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClientProjectDetailsViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_project_details_parent, parent, false)
        return ClientProjectDetailsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return projectDetailsList.size
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: ClientProjectDetailsViewHolder, position: Int) {
        val details = projectDetailsList[position]

        holder.expandableContent.removeAllViews()

        for ((key, value) in details.details) {

            val detailLayout = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_profile_detail, null) as LinearLayout

            val detailNameTextView = detailLayout.findViewById<TextView>(R.id.detailName)
            val detailValueTextView = detailLayout.findViewById<TextView>(R.id.detailValue)

            detailNameTextView.text = key
            detailValueTextView.text = value
            holder.expandableContent.addView(detailLayout)
        }
    }
}
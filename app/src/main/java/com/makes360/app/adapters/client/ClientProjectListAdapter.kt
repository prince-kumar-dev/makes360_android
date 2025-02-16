package com.makes360.app.adapters.client

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.makes360.app.R
import com.makes360.app.models.client.ProjectListDetailsData
import java.text.SimpleDateFormat
import java.util.Locale

class ClientProjectListAdapter(
    private val context: Context,
    private var projectList: MutableList<ProjectListDetailsData>, // Mutable list for updates
    private val onProjectSelected: (String, String) -> Unit // Callback for selection
) : RecyclerView.Adapter<ClientProjectListAdapter.ProjectListViewHolder>() {

    private var selectedPosition: Int = -1 // Store selected item index

    inner class ProjectListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectName: TextView = itemView.findViewById(R.id.projectTitle)
        val selectedIcon: ImageView = itemView.findViewById(R.id.selectedIcon)
        val projectCardView: MaterialCardView = itemView.findViewById(R.id.projectDetailsCardView)
        val expandableContent: LinearLayout = itemView.findViewById(R.id.expandable_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectListViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_project_list_parent, parent, false)
        return ProjectListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectListViewHolder, position: Int) {
        val project = projectList[position]

        holder.projectName.text = project.title
        holder.selectedIcon.visibility =
            if (position == selectedPosition) View.VISIBLE else View.GONE

        val renewalPeriodList =
            listOf("Monthly", "Quarterly", "Half Yearly", "Yearly", "One Time Payment")
        val statusList = listOf("In Progress", "On Hold", "Completed", "Waiting Respond")
        val progressValues = mapOf(0 to 50, 1 to 25, 2 to 100, 3 to 10)

        fun String?.orPlaceholder() = if (this.isNullOrEmpty()) "Will be Updated" else this

        // Clear existing views to prevent duplication
        holder.expandableContent.removeAllViews()

        val detailLayout = LayoutInflater.from(holder.itemView.context)
            .inflate(
                R.layout.item_project_list_child,
                holder.expandableContent,
                false
            ) as LinearLayout

        val progressBar = detailLayout.findViewById<ProgressBar>(R.id.projectStatusProgress)
        val statusText = detailLayout.findViewById<TextView>(R.id.projectStatusValue)
        val projectStartDate = detailLayout.findViewById<TextView>(R.id.projectStartDate)
        val firstRenewalDate = detailLayout.findViewById<TextView>(R.id.firstRenewalDate)
        val projectNote = detailLayout.findViewById<TextView>(R.id.projectNote)
        val gstNo = detailLayout.findViewById<TextView>(R.id.gstNo)

        val details = project.details

        val statusIndex = details["Current Status"]?.toIntOrNull() ?: 0
        val progress = progressValues[statusIndex] ?: 0
        val status = statusList.getOrNull(statusIndex).orPlaceholder()

        // Animate progress change
        if (progressBar.progress != progress) {
            progressBar.progress = progress
            ObjectAnimator.ofInt(progressBar, "progress", progress).apply {
                duration = 500
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        setStatusColors(holder.projectCardView, statusIndex, holder)
        
        statusText.text = status
        projectStartDate.text = "Start: ${formatDate(details["Start Date"]).orPlaceholder()}"
        firstRenewalDate.text = "1ˢᵗ Renew: ${formatDate(details["First Renewal Date"]).orPlaceholder()} (${renewalPeriodList[details["Renewal Period"]!!.toInt()].orPlaceholder()})"
        if (details["Project Note"] == "") {
            projectNote.visibility = View.GONE
        } else {
            projectNote.text = details["Project Note"]
        }
        gstNo.text = "Gst No: ${if (details["GST No"] == "0") "Will be Updated" else details["GST No"]}"

        // Add the fixed detail layout only once
        holder.expandableContent.addView(detailLayout)

        // Handle item click
        holder.itemView.setOnClickListener {
            val clickedPosition = holder.adapterPosition

            if (selectedPosition == clickedPosition) {
                // Deselect if clicked again
                selectedPosition = -1
                onProjectSelected("", "") // Clear projectId and title
            } else {
                // Select new item
                val previousSelected = selectedPosition
                selectedPosition = clickedPosition

                // Notify only affected items to improve performance
                if (previousSelected != -1) notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)

                // Pass the selected project_id to the callback
                val projectId = project.details["Project ID"]
                if (projectId != null) {
                    onProjectSelected(projectId, project.title)
                }
            }

            notifyItemChanged(clickedPosition)
        }

    }

    private fun setStatusColors(
        statusCardView: MaterialCardView,
        status: Int?,
        holder: ProjectListViewHolder,
    ) {

        when (status) {
            0 -> {
                statusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_orange)
                statusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_orange)
                )
            }

            1 -> {
                statusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_blue)
                statusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_blue)
                )
            }

            2 -> {
                statusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_green)
                statusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_green)
                )
            }

            3 -> {
                statusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_purple)
                statusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_purple)
                )
            }
        }
    }

    override fun getItemCount(): Int = projectList.size

    private fun formatDate(dateString: String?): String {
        return try {
            if (dateString.isNullOrEmpty() || dateString == "null") "Will be Updated"
            else SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault()).format(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)!!
            )
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
}
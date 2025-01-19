package com.makes360.app.adapters.client

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.client.ClientDetailsData
import com.makes360.app.ui.client.ClientProjectAssets
import com.makes360.app.ui.client.ClientProjectDetails
import com.makes360.app.ui.client.ClientContactLog

class ClientDetailsAdapter(
    private var context: Context,
    private val clientDetailsList: List<ClientDetailsData>
) : RecyclerView.Adapter<ClientDetailsAdapter.ClientDetailsViewHolder>() {

    inner class ClientDetailsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.detailsIcon)
        val title: TextView = itemView.findViewById(R.id.detailsTitle)
        val cardViewContainer: CardView = itemView.findViewById(R.id.detailsCardContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientDetailsViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_client_details, parent, false)
        return ClientDetailsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return clientDetailsList.size
    }

    override fun onBindViewHolder(holder: ClientDetailsViewHolder, position: Int) {
        val details = clientDetailsList[position]
        holder.icon.setImageResource(details.icon)
        holder.title.text = details.title

        holder.cardViewContainer.setOnClickListener {
            handleCardClick(details, holder.title.text.toString())
        }
    }

    private fun handleCardClick(details: ClientDetailsData, title: String) {
        when (title) {
            "Project Details" -> {
                if (details.projectId == "") {
                    showToast("Select Project from Project List")
                } else {
                    navigateToProjectDetails(details.projectId)
                }
            }

            "Project Assets" -> if (details.projectId == "") {
                showToast("Select Project from Project List")
            } else {
                navigateToProjectAssets(details.projectId, details.projectName)
            }

            "Contact Log" -> if (details.projectId == "") {
                showToast("Select Project from Project List")
            } else {
                navigateToContactLog(details.projectId, details.projectName)
            }

            "Service History" -> showToast("Service History")
            else -> showToast("Credentials")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToProjectDetails(projectId: String) {
        val intent = Intent(context, ClientProjectDetails::class.java).apply {
            putExtra("PROJECT_ID", projectId)
        }
        context.startActivity(intent)
    }

    private fun navigateToProjectAssets(projectId: String, projectName: String) {
        val intent = Intent(context, ClientProjectAssets::class.java).apply {
            putExtra("PROJECT_ID", projectId)
            putExtra("PROJECT_NAME", projectName)
        }
        context.startActivity(intent)
    }

    private fun navigateToContactLog(projectId: String, projectName: String) {
        val intent = Intent(context, ClientContactLog::class.java).apply {
            putExtra("PROJECT_ID", projectId)
            putExtra("PROJECT_NAME", projectName)
        }
        context.startActivity(intent)
    }

}
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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.makes360.app.R
import com.makes360.app.models.client.ClientDetailsData
import com.makes360.app.ui.client.ClientContactLog
import com.makes360.app.ui.client.ClientCredentials
import com.makes360.app.ui.client.ClientProfile
import com.makes360.app.ui.client.ClientProjectAssets
import com.makes360.app.ui.client.ClientServiceHistory

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
        if (details.title != "Profile Details") {
            holder.icon.setImageResource(details.icon)
        } else {
            when (details.icon) {
                0 -> {
                    holder.icon.setImageResource(R.drawable.ic_man_client)
                }

                1 -> {
                    holder.icon.setImageResource(R.drawable.ic_female_client)
                }

                2 -> {
                    // Using Glide with circle crop
                    Glide.with(context)
                        .load(details.profilePic)
                        .apply(RequestOptions.circleCropTransform()) // Apply circle crop transformation
                        .placeholder(R.drawable.circular_background) // Optional placeholder
                        .into(holder.icon)
                }
            }
        }

        holder.title.text = details.title

        holder.cardViewContainer.setOnClickListener {
            handleCardClick(details, holder.title.text.toString())
        }
    }

    private fun handleCardClick(details: ClientDetailsData, title: String) {
        when (title) {
            "Profile Details" -> {
                val intent = Intent(context, ClientProfile::class.java).apply {
                    putExtra("EMAIL", details.email)
                }
                context.startActivity(intent)
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

            "Service History" -> if (details.projectId == "") {
                showToast("Select Project from Project List")
            } else {
                navigateToServiceHistory(details.projectId, details.projectName)
            }

            "Credentials" -> if (details.projectId == "") {
                showToast("Select Project from Project List")
            } else {
                navigateToCredentials(details.projectId, details.projectName)
            }
        }
    }

    private fun navigateToCredentials(projectId: String, projectName: String) {
        val intent = Intent(context, ClientCredentials::class.java).apply {
            putExtra("PROJECT_ID", projectId)
            putExtra("PROJECT_NAME", projectName)
        }
        context.startActivity(intent)
    }

    private fun navigateToServiceHistory(projectId: String, projectName: String) {
        val intent = Intent(context, ClientServiceHistory::class.java).apply {
            putExtra("PROJECT_ID", projectId)
            putExtra("PROJECT_NAME", projectName)
        }
        context.startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
package com.makes360.app.adapters.trainee

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.trainee.TraineeDetailsData
import com.makes360.app.ui.trainee.TraineeLeaderboard
import com.makes360.app.ui.trainee.TraineeProfile

class TraineeDetailsAdapter (
    private var context: Context,
    private val traineeDetailsList: List<TraineeDetailsData>
) : RecyclerView.Adapter<TraineeDetailsAdapter.TraineeDetailsViewHolder>() {

    inner class TraineeDetailsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.detailsIcon)
        val title: TextView = itemView.findViewById(R.id.detailsTitle)
        val cardViewContainer: CardView = itemView.findViewById(R.id.detailsCardContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TraineeDetailsViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_trainee_details, parent, false)
        return TraineeDetailsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return traineeDetailsList.size
    }

    override fun onBindViewHolder(holder: TraineeDetailsViewHolder, position: Int) {
        val details = traineeDetailsList[position]
        holder.icon.setImageResource(details.icon)
        holder.title.text = details.title

        holder.cardViewContainer.setOnClickListener {
            handleCardClick(details, holder.title.text.toString())
        }
    }

    private fun handleCardClick(details: TraineeDetailsData, title: String) {
        when (title) {
            "Profile Details" -> {
                navigateToTraineeDetails(details.email)
            }
            "Leaderboard" -> {
                navigateToLeaderboard()
            }
            "Offer Letter" -> {
                navigateToOfferLetter()
            }
            "Profile Update" -> {
                navigateToProfileUpdate()
            }
        }
    }

    private fun navigateToLeaderboard() {
        val intent = Intent(context, TraineeLeaderboard::class.java).apply {
        }
        context.startActivity(intent)
    }

    private fun navigateToTraineeDetails(email: String) {
        val intent = Intent(context, TraineeProfile::class.java).apply {
            putExtra("EMAIL", email)
        }
        context.startActivity(intent)
    }

    private fun navigateToOfferLetter() {
        val url = "https://www.makes360.com/training/final-year-internship/offer-letter"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun navigateToProfileUpdate() {
        val url = "https://www.makes360.com/training/final-year-internship/detials-update/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
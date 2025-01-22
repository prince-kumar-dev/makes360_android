package com.makes360.app.adapters.trainee

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
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.trainee.TraineeDetailsData
import com.makes360.app.ui.trainee.TraineeFeeInfo
import com.makes360.app.ui.trainee.TraineeLeaderboard
import com.makes360.app.ui.trainee.TraineeProfile
import com.makes360.app.ui.trainee.TraineeSupport

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
            "Schedule" -> {
                navigateToSchedule()
            }
            "Update Profile" -> {
                navigateToProfileUpdate()
            }
            "Profile Update" -> {
                Toast.makeText(context, "Profile already updated", Toast.LENGTH_SHORT).show()
            }
            "Fee Info" -> {
                navigateToFeeInfo(details.email)
            }
            "Feedback" -> {
                navigateToFeedback()
            }
            "Support" -> {
                navigateToSupport()
            }
            "Attendance" -> {
                navigateToAttendance()
            }
        }
    }

    private fun navigateToSupport() {
        val intent = Intent(context, TraineeSupport::class.java).apply {
        }
        context.startActivity(intent)
    }

    private fun navigateToAttendance() {
        val url = "https://docs.google.com/spreadsheets/d/1LK0ZriNyIhAnwgTZzvUuJC_0oKqQwIoc27GvniKCw3s"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun navigateToFeedback() {
        val url = "https://forms.gle/SEj54WHqPfjTtrCq9"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
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

    private fun navigateToSchedule() {
        val url = "https://docs.google.com/document/d/1TPH4N8FWqY83rU7Ha1iEublt1iga-LPoSnmt3avCIEg/edit?usp=sharing"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun navigateToProfileUpdate() {
        val url = "https://www.makes360.com/training/final-year-internship/detials-update/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun navigateToFeeInfo(email: String) {
        val intent = Intent(context, TraineeFeeInfo::class.java).apply {
            putExtra("EMAIL", email)
        }
        context.startActivity(intent)
    }
}
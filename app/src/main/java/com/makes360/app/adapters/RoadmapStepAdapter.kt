package com.makes360.app.adapters

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.RoadmapStep
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RoadmapStepAdapter(private val steps: List<RoadmapStep>) :
    RecyclerView.Adapter<RoadmapStepAdapter.RoadmapViewHolder>() {

    inner class RoadmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stepIcon: ImageView = itemView.findViewById(R.id.stepIcon)
        val stepTitle: TextView = itemView.findViewById(R.id.stepTitle)
        val stepTitleCardView: View = itemView.findViewById(R.id.stepTitleCardView)
        val progressBar: View = itemView.findViewById(R.id.progressBar)
        var handler: Handler? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoadmapViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_roadmap, parent, false)
        return RoadmapViewHolder(view)
    }

    override fun onViewRecycled(holder: RoadmapViewHolder) {
        super.onViewRecycled(holder)
        holder.handler?.removeCallbacksAndMessages(null)
        holder.handler = null
    }

    override fun onBindViewHolder(holder: RoadmapViewHolder, position: Int) {
        val step = steps[position]

        val dateString = when {
            step.appliedDate.isNotEmpty() -> step.appliedDate
            step.shortListedDate.isNotEmpty() -> step.shortListedDate
            step.interviewCallDate.isNotEmpty() -> step.interviewCallDate
            step.joiningDate.isNotEmpty() -> step.joiningDate
            step.passedOutDate.isNotEmpty() -> step.passedOutDate
            else -> ""
        }

        // Show title initially
        holder.stepTitle.text = step.title

        if (step.isCompleted && dateString.isNotEmpty()) {
            startToggleCycle(holder, step.title, dateString)
        }

        if (step.isCompleted) {
            holder.stepIcon.setImageResource(R.drawable.ic_step_icon)
            if (step.applicationStatus == 3) {
                holder.progressBar.setBackgroundColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.material_flat_red_dark
                    )
                )
            } else {
                holder.progressBar.setBackgroundColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.text_primary
                    )
                )
            }

            (holder.stepTitleCardView as androidx.cardview.widget.CardView).setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, step.cardViewBgColor)
            )
            holder.stepIcon.clearColorFilter()
        } else {
            holder.stepIcon.setImageResource(R.drawable.ic_step_icon)
            holder.stepIcon.setColorFilter(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.gray_font
                )
            )
            (holder.stepTitleCardView as androidx.cardview.widget.CardView).setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.gray_font)
            )
            holder.progressBar.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.gray_font)
            )
        }
    }

    // Start Toggle Cycle Logic
    private fun startToggleCycle(holder: RoadmapViewHolder, title: String, dateString: String) {
        holder.handler?.removeCallbacksAndMessages(null) // Remove any existing callbacks
        holder.handler = Handler()

        val appliedAgoText = calculateTimeAgo(dateString, title)

        lateinit var showTitleRunnable: Runnable
        lateinit var showDateRunnable: Runnable

        showTitleRunnable = Runnable {
            holder.stepTitle.text = title
            holder.handler?.postDelayed(showDateRunnable, 3000) // Show title for 3 seconds
        }

        showDateRunnable = Runnable {
            holder.stepTitle.text = appliedAgoText
            holder.handler?.postDelayed(showTitleRunnable, 2000) // Show date for 2 second
        }

        holder.handler?.post(showTitleRunnable) // Start with title
    }

    // Calculate time ago
    private fun calculateTimeAgo(dateString: String, title: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val date = format.parse(dateString)
            val currentDate = Date()
            val diff = currentDate.time - date.time

            val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
            val months = days / 30
            val remainingDays = days % 30

            if(title == "Doing Internship") {
                when {
                    months > 0 -> "Since $months Month${if (months > 1) "s" else ""} $remainingDays Day${if (remainingDays > 1) "s" else ""}"
                    days > 0 -> "Since $days Day${if (days > 1) "s" else ""} ago"
                    else -> "From Today"
                }
            } else {
                when {
                    months > 0 -> "$months Month${if (months > 1) "s" else ""} $remainingDays Day${if (remainingDays > 1) "s" else ""} ago"
                    days > 0 -> "$days Day${if (days > 1) "s" else ""} ago"
                    else -> "Today"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    override fun getItemCount(): Int = steps.size
}
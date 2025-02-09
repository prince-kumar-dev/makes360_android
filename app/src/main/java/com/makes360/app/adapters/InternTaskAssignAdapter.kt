package com.makes360.app.adapters

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.makes360.app.R
import com.makes360.app.models.InternTaskAssignData
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class InternTaskAssignAdapter(
    private var taskList: List<InternTaskAssignData>
) : RecyclerView.Adapter<InternTaskAssignAdapter.TaskViewHolder>() {

    fun updateTaskList(newList: List<InternTaskAssignData>) {
        taskList = newList
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val internTaskCardView: MaterialCardView = view.findViewById(R.id.internTaskCardView)
        val dueAlertTextView: MaterialTextView = view.findViewById(R.id.dueAlertTextView)
        val taskName: MaterialTextView = view.findViewById(R.id.taskName)
        val taskDescription: MaterialTextView = view.findViewById(R.id.taskDescription)
        val taskPriorityCardView: MaterialCardView = view.findViewById(R.id.taskPriorityCardView)
        val taskPriority: MaterialTextView = view.findViewById(R.id.taskPriorityTextView)
        val taskStatusCardView: MaterialCardView = view.findViewById(R.id.taskStatusCardView)
        val taskStatus: MaterialTextView = view.findViewById(R.id.taskStatusTextView)
        val startDate: MaterialTextView = view.findViewById(R.id.startDate)
        val dueDate: MaterialTextView = view.findViewById(R.id.dueDate)
        val progressBar: ProgressBar = view.findViewById(R.id.internTaskStatusProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_intern_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskName.text = task.name
        holder.taskDescription.apply {
            visibility =
                if (task.description.isEmpty() || task.description == "null") View.GONE else View.VISIBLE
            text = task.description
        }

        holder.taskPriority.text = task.priority
        holder.taskStatus.text = task.status
        holder.startDate.text = "Start: ${formatDate(task.startDate)}"
        holder.dueDate.text = "Due: ${formatDate(task.dueDate)}"

        // Set priority color
        setPriorityColors(holder, task)

        // Set status color
        setStatusColors(holder, task)

        // Handling overdue & soon-due tasks
        if (task.dueDate != "Not Given") {
            val daysLeft = getDaysLeft(task.dueDate)
            if (daysLeft <= 5 && task.status != "Completed") {
                holder.dueAlertTextView.visibility = View.VISIBLE
                holder.dueAlertTextView.text = when {
                    daysLeft > 0 -> "Due in $daysLeft days ⏳"
                    daysLeft == 0L -> "Due today ⚠️"
                    else -> "Task Overdue ⏰"
                }
                holder.internTaskCardView.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.material_flat_red)
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_red))
                }
            } else {
                // Reset card color when not due soon
                holder.dueAlertTextView.visibility = View.GONE
                holder.internTaskCardView.apply {
                    strokeColor = ContextCompat.getColor(
                        context,
                        R.color.black
                    ) // Reset to default stroke color
                    setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    ) // Reset to default background color
                }
            }
        } else {
            // Reset card color for tasks with no due date
            holder.dueAlertTextView.visibility = View.GONE
            holder.internTaskCardView.apply {
                strokeColor = ContextCompat.getColor(context, R.color.black)
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            }
        }

        // Set progress bar
        setUpProgressBar(holder, task)
    }

    private fun setPriorityColors(holder: TaskViewHolder, task: InternTaskAssignData) {
        when (task.priority) {
            "Low" -> {
                holder.taskPriorityCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_blue)
                holder.taskPriorityCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_blue)
                )
            }

            "Medium" -> {
                holder.taskPriorityCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_yellow)
                holder.taskPriorityCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_yellow)
                )
            }

            "High" -> {
                holder.taskPriorityCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_orange)
                holder.taskPriorityCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_orange)
                )
            }

            "Urgent" -> {
                holder.taskPriorityCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_red)
                holder.taskPriorityCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_red)
                )
            }
        }
    }

    private fun setStatusColors(holder: TaskViewHolder, task: InternTaskAssignData) {
        when (task.status) {
            "Not Started" -> {
                holder.taskStatusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_blue)
                holder.taskStatusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_blue)
                )
            }

            "In Progress" -> {
                holder.taskStatusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_yellow)
                holder.taskStatusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_yellow)
                )
            }

            "Testing" -> {
                holder.taskStatusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_orange)
                holder.taskStatusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_orange)
                )
            }

            "Awaiting Feedback" -> {
                holder.taskStatusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_purple)
                holder.taskStatusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_purple)
                )
            }

            "Completed" -> {
                holder.taskStatusCardView.strokeColor =
                    holder.itemView.context.getColor(R.color.material_core_green)
                holder.taskStatusCardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.light_green)
                )
            }
        }
    }

    override fun getItemCount(): Int = taskList.size

    private fun setUpProgressBar(holder: TaskViewHolder, task: InternTaskAssignData) {
        val progress = mapOf(
            "Not Started" to 0,
            "In Progress" to 25,
            "Testing" to 50,
            "Awaiting Feedback" to 75,
            "Completed" to 100
        )[task.status] ?: 0

        val progressBar = holder.progressBar
        if (progressBar.progress != progress) {
            progressBar.progress = progress
            ObjectAnimator.ofInt(progressBar, "progress", progress).apply {
                duration = 500
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    private fun getDaysLeft(dueDate: String): Long {
        return parseDate(dueDate)?.let {
            val diff = it.time - Calendar.getInstance().timeInMillis
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        } ?: Long.MAX_VALUE
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault())
            outputFormat.format(inputFormat.parse(dateStr)!!)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}
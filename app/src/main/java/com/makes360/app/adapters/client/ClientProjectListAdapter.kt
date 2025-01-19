package com.makes360.app.adapters.client

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.client.ProjectListDetailsData

class ClientProjectListAdapter(
    private val context: Context,
    private var projectList: List<ProjectListDetailsData>, // Mutable list for rearranging
    private val onProjectSelected: (String, String) -> Unit // Callback for selection
) : RecyclerView.Adapter<ClientProjectListAdapter.ProjectListViewHolder>() {

    inner class ProjectListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectName: TextView = itemView.findViewById(R.id.project_name)
        val projectIcon: ImageView = itemView.findViewById(R.id.project_icon)
        val selectedIcon: ImageView = itemView.findViewById(R.id.selected_icon) // New selected icon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectListViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_project_list_child, parent, false)
        return ProjectListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectListViewHolder, position: Int) {
        val project = projectList[position]

        holder.projectName.text = project.title
        holder.projectIcon.setImageResource(project.icon)

        // Show or hide the selected icon based on the isSelected property
        holder.selectedIcon.visibility = if (project.isSelected) View.VISIBLE else View.GONE

        // Handle item click
        holder.itemView.setOnClickListener {
            // Update the selected state
            projectList.forEach { it.isSelected = false } // Unselect all projects
            project.isSelected = true // Select the clicked project

//            // Move the selected project to the top
//            projectList.removeAt(position)
//            projectList.add(0, project)
//
            // Notify changes to the adapter
            notifyDataSetChanged()

            // Pass the project_id to the callback
            val projectId = project.details["Project ID"]
            if (projectId != null) {
                onProjectSelected(projectId, project.title)
            }
        }
    }

    override fun getItemCount(): Int {
        return projectList.size
    }
}
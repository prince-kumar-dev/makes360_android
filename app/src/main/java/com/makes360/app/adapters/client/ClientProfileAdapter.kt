package com.makes360.app.adapters.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.client.ClientProfileCategory

class ClientProfileAdapter (private val categories: List<ClientProfileCategory>
) : RecyclerView.Adapter<ClientProfileAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.category_name)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrow_icon)
        val expandableContent: LinearLayout = itemView.findViewById(R.id.expandable_content)
        val categoryIcon: ImageView = itemView.findViewById(R.id.category_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.categoryName.text = category.title
        holder.expandableContent.removeAllViews()

        for ((key, value) in category.details) {
            val detailLayout = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_profile_detail, null) as LinearLayout

            val detailNameTextView = detailLayout.findViewById<TextView>(R.id.detailName)
            val detailValueTextView = detailLayout.findViewById<TextView>(R.id.detailValue)

            detailNameTextView.text = key
            detailValueTextView.text = value
            holder.expandableContent.addView(detailLayout)

            holder.expandableContent.visibility = if (category.isExpanded) View.VISIBLE else View.GONE
            holder.arrowIcon.setImageResource(
                if (category.isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow
            )

            holder.itemView.findViewById<LinearLayout>(R.id.header_layout).setOnClickListener {
                category.isExpanded = !category.isExpanded
                notifyItemChanged(position)
            }

            holder.categoryIcon.setImageResource(category.icon)
        }
    }
}
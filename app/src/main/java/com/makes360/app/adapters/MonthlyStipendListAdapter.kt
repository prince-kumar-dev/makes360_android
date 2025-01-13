package com.makes360.app.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.MonthlyStipendData
import com.makes360.app.ui.intern.Stipend

class MonthlyStipendListAdapter(
    val context: Context,
    private val monthlyStipendDataList: List<MonthlyStipendData>
) :
    RecyclerView.Adapter<MonthlyStipendListAdapter.MonthlyStipendParentViewHolder>() {

    inner class MonthlyStipendParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.monthlyStipendParentTitleTv)
        val logo: ImageView = itemView.findViewById(R.id.monthlyStipendParentLogoIv)
        val stipendView: CardView = itemView.findViewById(R.id.stipendSeeCardView)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MonthlyStipendParentViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_monthly_stipend_list, parent, false)
        return MonthlyStipendParentViewHolder(view)
    }

    override fun getItemCount(): Int {
        return monthlyStipendDataList.size
    }

    override fun onBindViewHolder(holder: MonthlyStipendParentViewHolder, position: Int) {
        val parentData = monthlyStipendDataList[position]
        holder.title.text = parentData.title
        holder.logo.setImageResource(parentData.logo)

        holder.stipendView.setOnClickListener {
            val month = parentData.idMonth
            val year = parentData.idYear
            val mail = parentData.email

            val intent = Intent(context, Stipend::class.java)
            intent.putExtra("MONTH", month)
            intent.putExtra("YEAR", year)
            intent.putExtra("EMAIL", mail)
            context.startActivity(intent)
        }
    }
}
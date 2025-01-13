package com.makes360.app.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.DownloadContent

class DownloadContentAdapter(
    private val downloadContentList: List<DownloadContent>
) : RecyclerView.Adapter<DownloadContentAdapter.DownloadContentViewHolder>() {

    inner class DownloadContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val downloadContentBtn: LinearLayout = itemView.findViewById(R.id.downloadContentBtn)
        val iconImg: ImageView = itemView.findViewById(R.id.iconImg)
        val downloadContentHeading: TextView = itemView.findViewById(R.id.downloadContentHeading)
        val downloadContentDes: TextView = itemView.findViewById(R.id.downloadContentDes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadContentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content_download, parent, false)
        return DownloadContentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadContentViewHolder, position: Int) {
        val content = downloadContentList[position]
        holder.iconImg.setImageResource(content.iconResId)
        holder.downloadContentHeading.text = content.heading
        holder.downloadContentDes.text = content.description

        holder.downloadContentBtn.setOnClickListener {
            val url = content.url
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            holder.itemView.context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int = downloadContentList.size
}

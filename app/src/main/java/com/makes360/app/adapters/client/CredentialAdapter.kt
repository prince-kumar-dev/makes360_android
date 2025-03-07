package com.makes360.app.adapters.client

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.makes360.app.R
import com.makes360.app.models.client.CredentialData

class CredentialAdapter(private val credentials: List<CredentialData>, private val context: Context) :
    RecyclerView.Adapter<CredentialAdapter.CredentialViewHolder>() {

    inner class CredentialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvCredentialName)
        val value: TextView = view.findViewById(R.id.tvCredentialValue)
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val copyButton: ImageView = view.findViewById(R.id.btnCopy)
        val toggleButton: ImageView = view.findViewById(R.id.btnToggleVisibility)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_credential, parent, false)
        return CredentialViewHolder(view)
    }

    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {
        val credential = credentials[position]
        holder.name.text = credential.name

        // Initially hide the password
        holder.value.text = "******"
        var isPasswordVisible = false

        // Set different icons based on credential type
        holder.icon.setImageResource(getIconForCredential(credential.name))

        // Toggle password visibility
        holder.toggleButton.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                holder.value.text = credential.value
                holder.toggleButton.setImageResource(R.drawable.ic_eye_open)
            } else {
                holder.value.text = "******"
                holder.toggleButton.setImageResource(R.drawable.ic_eye_closed)
            }
        }

        // Copy to clipboard functionality
        holder.copyButton.setOnClickListener {
            copyToClipboard(credential.name, credential.value)
        }
    }

    override fun getItemCount(): Int = credentials.size

    // Function to get appropriate icon based on credential type
    private fun getIconForCredential(name: String): Int {
        return when {
            name.contains("email", true) -> R.drawable.ic_email
            name.contains("facebook", true) -> R.drawable.ic_facebook
            name.contains("instagram", true) -> R.drawable.ic_instagram
            name.contains("linkedin", true) -> R.drawable.ic_linkedin
            name.contains("pinterest", true) -> R.drawable.ic_pinterest
            name.contains("twitter", true) -> R.drawable.ic_x
            name.contains("youtube", true) -> R.drawable.ic_youtube
            else -> R.drawable.ic_key
        }
    }

    // Function to copy value to clipboard
    private fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
package com.example.medicare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class PharmacyAdapter(private val items: List<PharmacyItem>) :
    RecyclerView.Adapter<PharmacyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txt_pharmacy_name)
        val txtRating: TextView = view.findViewById(R.id.txt_rating_value)
        val txtDetails: TextView = view.findViewById(R.id.txt_pharmacy_details)
        val btnNavigate: View = view.findViewById(R.id.btn_navigate)
        val btnCall: View = view.findViewById(R.id.btn_call)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pharmacy, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.name
        holder.txtRating.text = item.rating
        holder.txtDetails.text = item.details

        val context = holder.itemView.context

        // Card click details dialog
        holder.itemView.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(item.name)
                .setMessage("Rating: ${item.rating} Stars\nDistance: ${item.details}\nHours: Open Daily 08:00 AM - 10:00 PM")
                .setPositiveButton("Close", null)
                .show()
        }

        // Navigate button click action
        holder.btnNavigate.setOnClickListener {
            Toast.makeText(context, "Navigation feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Call button click action
        holder.btnCall.setOnClickListener {
            Toast.makeText(context, "Calling pharmacy...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = items.size
}

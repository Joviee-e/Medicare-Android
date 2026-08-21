package com.example.medicare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PharmacyAdapter(
    private val items: List<PharmacyItem>,
    private val onItemClick: (PharmacyItem) -> Unit,
    private val onNavigateClick: (PharmacyItem) -> Unit,
    private val onCallClick: (PharmacyItem) -> Unit
) : RecyclerView.Adapter<PharmacyAdapter.ViewHolder>() {

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

        // Show/hide call button based on phone availability
        if (item.phoneNumber.isNullOrEmpty()) {
            holder.btnCall.visibility = View.GONE
        } else {
            holder.btnCall.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.btnNavigate.setOnClickListener {
            onNavigateClick(item)
        }

        holder.btnCall.setOnClickListener {
            onCallClick(item)
        }
    }

    override fun getItemCount() = items.size
}

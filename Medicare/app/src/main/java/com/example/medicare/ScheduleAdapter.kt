package com.example.medicare

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(private val items: List<ScheduleItem>) :
    RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.img_status_icon)
        val txtName: TextView = view.findViewById(R.id.txt_medicine_name)
        val txtDose: TextView = view.findViewById(R.id.txt_medicine_dose)
        val txtStatus: TextView = view.findViewById(R.id.txt_status)
        val txtTime: TextView = view.findViewById(R.id.txt_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.name
        holder.txtDose.text = item.dose
        holder.txtTime.text = item.time
        holder.txtStatus.text = item.status

        val context = holder.itemView.context
        when (item.status) {
            "Taken" -> {
                holder.imgIcon.setImageResource(R.drawable.ic_check_circle)
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_taken)
                holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.status_taken))
            }
            "Upcoming" -> {
                holder.imgIcon.setImageResource(R.drawable.ic_clock)
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.status_scheduled))
            }
            "Missed" -> {
                holder.imgIcon.setImageResource(R.drawable.ic_close_circle)
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_missed)
                holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.status_missed))
            }
            else -> { // Handles other states like Skipped/Snoozed dynamically
                holder.imgIcon.setImageResource(R.drawable.ic_clock)
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_upcoming)
                holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.primary))
            }
        }

        // Click to launch ReminderAlarmActivity with specific medication parameters
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ReminderAlarmActivity::class.java).apply {
                putExtra("med_id", item.id)
                putExtra("med_name", item.name)
                putExtra("med_dose", item.dose)
                putExtra("med_time", item.time)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size
}

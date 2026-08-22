package com.example.medicare

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.api.BaseResponse
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MedicineAdapter(private val items: MutableList<MedicineItem>) :
    RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val strip: View = view.findViewById(R.id.view_status_strip)
        val imgMedIcon: ImageView = view.findViewById(R.id.img_med_icon)
        val iconBg: androidx.cardview.widget.CardView = view.findViewById(R.id.med_icon_bg)
        val txtName: TextView = view.findViewById(R.id.txt_med_name)
        val txtDose: TextView = view.findViewById(R.id.txt_med_dose)
        val imgStatusSmall: ImageView = view.findViewById(R.id.img_status_small)
        val txtStatusInfo: TextView = view.findViewById(R.id.txt_status_info)
        val btnEdit: ImageView = view.findViewById(R.id.btn_edit)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.name
        holder.txtDose.text = item.dose
        holder.txtStatusInfo.text = item.info

        val context = holder.itemView.context

        // Default alpha
        holder.txtName.alpha = 1.0f
        holder.txtDose.alpha = 1.0f

        when (item.statusColor) {
            "Teal" -> {
                holder.strip.setBackgroundColor(ContextCompat.getColor(context, R.color.primary))
                holder.iconBg.setCardBackgroundColor(ContextCompat.getColor(context, R.color.secondary_container))
                holder.imgMedIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary))
                
                holder.imgStatusSmall.setImageResource(R.drawable.ic_clock)
                holder.imgStatusSmall.setColorFilter(ContextCompat.getColor(context, R.color.primary))
                holder.txtStatusInfo.setTextColor(ContextCompat.getColor(context, R.color.primary))
            }
            "Red" -> {
                holder.strip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_missed))
                holder.iconBg.setCardBackgroundColor(ContextCompat.getColor(context, R.color.secondary_container))
                holder.imgMedIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary))
                
                holder.imgStatusSmall.setImageResource(R.drawable.ic_warning)
                holder.imgStatusSmall.setColorFilter(ContextCompat.getColor(context, R.color.status_missed))
                holder.txtStatusInfo.setTextColor(ContextCompat.getColor(context, R.color.status_missed))
            }
            "Grey" -> {
                holder.strip.setBackgroundColor(ContextCompat.getColor(context, R.color.neutral_gray))
                holder.iconBg.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container))
                holder.imgMedIcon.setColorFilter(ContextCompat.getColor(context, R.color.neutral_gray))
                
                holder.imgStatusSmall.setImageResource(R.drawable.ic_check_circle)
                holder.imgStatusSmall.setColorFilter(ContextCompat.getColor(context, R.color.neutral_gray))
                holder.txtStatusInfo.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                
                // Dim text for taken medications
                holder.txtName.alpha = 0.5f
                holder.txtDose.alpha = 0.5f
            }
        }

        // Edit button click logic -> opens AddMedicineActivity with pre-fill extras
        holder.btnEdit.setOnClickListener {
            val intent = Intent(context, AddMedicineActivity::class.java).apply {
                putExtra("med_id", item.id)
                putExtra("med_name", item.name)
                putExtra("med_dose", item.dose)
                putExtra("med_info", item.info)
            }
            context.startActivity(intent)
        }

        // Delete button click logic -> confirmation Dialog and runtime list update
        holder.btnDelete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Delete this medicine?")
                    .setMessage("Are you sure you want to delete ${items[pos].name}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        val medId = items[pos].id
                        
                        // 1. Cancel alarms immediately on device
                        AlarmScheduler.cancelAlarms(context, medId, 10)
                        
                        Toast.makeText(context, "Deleting medication...", Toast.LENGTH_SHORT).show()

                        RetrofitClient.getApiService(context).deleteMedicine(medId)
                            .enqueue(object : Callback<BaseResponse> {
                                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        // 2. SUCCESS: Remove from cache and UI
                                        MedicineCache.removeMedicine(context, medId)
                                        Toast.makeText(context, "Medicine deleted", Toast.LENGTH_SHORT).show()
                                        
                                        // Safety check: make sure position is still valid before removing
                                        val currentPos = holder.adapterPosition
                                        if (currentPos != RecyclerView.NO_POSITION && currentPos < items.size) {
                                            items.removeAt(currentPos)
                                            notifyItemRemoved(currentPos)
                                            notifyItemRangeChanged(currentPos, itemCount)
                                        }
                                    } else {
                                        // 3. FAILURE: Keep cache, restore alarms, show error
                                        val cachedMed = MedicineCache.getMedicine(medId)
                                        if (cachedMed != null) {
                                            AlarmScheduler.scheduleAlarms(context, cachedMed)
                                        }
                                        val errMsg = RetrofitClient.parseErrorMessage(response)
                                        Toast.makeText(context, "Failed to delete: $errMsg", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                                    // 3. FAILURE: Keep cache, restore alarms, show error
                                    val cachedMed = MedicineCache.getMedicine(medId)
                                    if (cachedMed != null) {
                                        AlarmScheduler.scheduleAlarms(context, cachedMed)
                                    }
                                    Toast.makeText(context, "Network error deleting medicine", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                    .show()
            }
        }
    }

    override fun getItemCount() = items.size
}

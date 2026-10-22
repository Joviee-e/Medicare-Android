package com.example.medicare

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.api.GetNotificationsResponse
import com.example.medicare.api.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NotificationHelper {

    data class NotificationItem(
        val id: String,
        val title: String,
        val message: String,
        val time: String,
        val type: String, // "allergy", "interaction", "warning", "guidance", "adherence", "reminder", "refill", "health"
        val priority: String = "low", // "high", "medium", "low"
        val actionLabel: String = "Explain with AI",
        val contextForAi: String? = null,
        val medicineId: String? = null
    )

    // In-memory prioritized notifications list
    val activeNotifications = mutableListOf<NotificationItem>()

    // Priority ordering helper
    private fun priorityRank(p: String): Int = when (p.lowercase()) {
        "high" -> 0
        "medium" -> 1
        else -> 2
    }

    /**
     * Fetch live prioritized notifications from the backend and merge into active list.
     */
    fun syncNotifications(context: Context, onComplete: (() -> Unit)? = null) {
        val sessionManager = SessionManager(context)
        if (!sessionManager.isLoggedIn()) {
            onComplete?.invoke()
            return
        }

        RetrofitClient.getApiService(context).getNotifications(unreadOnly = false)
            .enqueue(object : Callback<GetNotificationsResponse> {
                override fun onResponse(call: Call<GetNotificationsResponse>, response: Response<GetNotificationsResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        activeNotifications.clear()
                        for (n in body.notifications) {
                            val actionTxt = when (n.type) {
                                "allergy", "interaction", "warning", "guidance", "adherence" -> "Explain with AI"
                                "reminder" -> "Log Dose"
                                "refill" -> "View Supply"
                                else -> "Review"
                            }
                            val timeStr = n.createdAt?.take(10) ?: "Recent"
                            activeNotifications.add(
                                NotificationItem(
                                    id = n.id,
                                    title = n.title,
                                    message = n.message,
                                    time = timeStr,
                                    type = n.type,
                                    priority = n.priority,
                                    actionLabel = actionTxt,
                                    contextForAi = n.contextForAi,
                                    medicineId = n.medicineId
                                )
                            )
                        }
                        // Sort by priority (high > medium > low)
                        activeNotifications.sortBy { priorityRank(it.priority) }
                    }
                    onComplete?.invoke()
                }

                override fun onFailure(call: Call<GetNotificationsResponse>, t: Throwable) {
                    onComplete?.invoke()
                }
            })
    }

    fun addNotification(item: NotificationItem) {
        // Prevent duplicate IDs
        activeNotifications.removeAll { it.id == item.id }
        activeNotifications.add(0, item)
        activeNotifications.sortBy { priorityRank(it.priority) }
    }

    fun show(activity: FragmentActivity) {
        // First sync with backend, then show sheet
        syncNotifications(activity) {
            val sheet = NotificationBottomSheet()
            sheet.show(activity.supportFragmentManager, "NotificationBottomSheet")
        }
    }

    class NotificationBottomSheet : BottomSheetDialogFragment() {

        private lateinit var recycler: RecyclerView
        private lateinit var emptyLayout: LinearLayout
        private lateinit var btnClearAll: TextView
        private var currentFilter = "all"
        private val displayedItems = mutableListOf<NotificationItem>()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.dialog_notifications, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            recycler = view.findViewById(R.id.recycler_notifications)
            emptyLayout = view.findViewById(R.id.layout_empty_notifs)
            btnClearAll = view.findViewById(R.id.btn_clear_all_notifs)

            recycler.layoutManager = LinearLayoutManager(requireContext())

            // Setup Filter Chips
            val chipAll = view.findViewById<Chip>(R.id.chip_notif_all)
            val chipReminders = view.findViewById<Chip>(R.id.chip_notif_reminders)
            val chipRefills = view.findViewById<Chip>(R.id.chip_notif_refills)
            val chipHealth = view.findViewById<Chip>(R.id.chip_notif_health)

            chipAll.setOnClickListener {
                currentFilter = "all"
                applyFilter()
            }
            chipReminders.setOnClickListener {
                currentFilter = "reminder"
                applyFilter()
            }
            chipRefills.setOnClickListener {
                currentFilter = "refill"
                applyFilter()
            }
            chipHealth.setOnClickListener {
                currentFilter = "health"
                applyFilter()
            }

            btnClearAll.setOnClickListener {
                // Call backend clear endpoint
                RetrofitClient.getApiService(requireContext()).clearNotifications()
                    .enqueue(object : Callback<com.example.medicare.api.BaseResponse> {
                        override fun onResponse(call: Call<com.example.medicare.api.BaseResponse>, response: Response<com.example.medicare.api.BaseResponse>) {}
                        override fun onFailure(call: Call<com.example.medicare.api.BaseResponse>, t: Throwable) {}
                    })
                activeNotifications.clear()
                applyFilter()
            }

            applyFilter()
        }

        private fun applyFilter() {
            displayedItems.clear()
            if (currentFilter == "all") {
                displayedItems.addAll(activeNotifications)
            } else if (currentFilter == "health") {
                // Include allergy, interaction, warning, guidance, adherence in health tab
                displayedItems.addAll(activeNotifications.filter { 
                    it.type in listOf("health", "allergy", "interaction", "warning", "guidance", "adherence") 
                })
            } else {
                displayedItems.addAll(activeNotifications.filter { it.type == currentFilter })
            }

            displayedItems.sortBy { priorityRank(it.priority) }

            if (displayedItems.isEmpty()) {
                recycler.visibility = View.GONE
                emptyLayout.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                emptyLayout.visibility = View.GONE
                recycler.adapter = NotificationAdapter(
                    items = displayedItems,
                    onActionClick = { item ->
                        dismiss()
                        // If finding relates to medication intelligence / safety, open Chatbot with structured context
                        if (item.type in listOf("allergy", "interaction", "warning", "guidance", "adherence") || item.contextForAi != null) {
                            val prompt = if (!item.contextForAi.isNullOrBlank()) {
                                "Can you explain this finding for \"${item.title}\"? Details: ${item.message}. What precautions or adherence recommendations should I follow based on my schedule and health predictions?"
                            } else {
                                "Can you explain this medication finding: \"${item.title}\" (${item.message}), and what precautions or steps I should take based on my medication routine?"
                            }
                            val intent = Intent(requireContext(), AIAssistantActivity::class.java).apply {
                                putExtra("ai_prompt", prompt)
                                putExtra("context_title", item.title)
                                putExtra("context_message", item.message)
                                putExtra("context_type", item.type)
                            }
                            startActivity(intent)
                        } else {
                            val intent = Intent(requireContext(), MedicinesActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            startActivity(intent)
                        }
                    },
                    onDismissClick = { item ->
                        activeNotifications.removeAll { it.id == item.id }
                        RetrofitClient.getApiService(requireContext()).markNotificationRead(item.id)
                            .enqueue(object : Callback<com.example.medicare.api.BaseResponse> {
                                override fun onResponse(call: Call<com.example.medicare.api.BaseResponse>, response: Response<com.example.medicare.api.BaseResponse>) {}
                                override fun onFailure(call: Call<com.example.medicare.api.BaseResponse>, t: Throwable) {}
                            })
                        applyFilter()
                    }
                )
            }
        }
    }

    class NotificationAdapter(
        private val items: List<NotificationItem>,
        private val onActionClick: (NotificationItem) -> Unit,
        private val onDismissClick: (NotificationItem) -> Unit
    ) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardIcon: MaterialCardView = itemView.findViewById(R.id.card_notif_icon)
            val imgType: ImageView = itemView.findViewById(R.id.img_notif_type)
            val txtTitle: TextView = itemView.findViewById(R.id.txt_notif_title)
            val txtMessage: TextView = itemView.findViewById(R.id.txt_notif_message)
            val txtTime: TextView = itemView.findViewById(R.id.txt_notif_time)
            val btnAction: MaterialButton = itemView.findViewById(R.id.btn_notif_action)
            val btnDismiss: ImageView = itemView.findViewById(R.id.btn_dismiss_notif)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val context = holder.itemView.context

            // Title with priority indicator
            if (item.priority == "high") {
                holder.txtTitle.text = "🔴 " + item.title
                holder.txtTitle.setTextColor(Color.parseColor("#B71C1C"))
                holder.cardIcon.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                holder.imgType.setColorFilter(Color.parseColor("#C62828"))
                holder.imgType.setImageResource(R.drawable.ic_warning)
            } else if (item.priority == "medium") {
                holder.txtTitle.text = "⚠️ " + item.title
                holder.txtTitle.setTextColor(Color.parseColor("#E65100"))
                holder.cardIcon.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                holder.imgType.setColorFilter(Color.parseColor("#EF6C00"))
                holder.imgType.setImageResource(R.drawable.ic_warning)
            } else {
                holder.txtTitle.text = item.title
                holder.txtTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                holder.cardIcon.setCardBackgroundColor(ContextCompat.getColor(context, R.color.secondary_container))
                holder.imgType.setColorFilter(ContextCompat.getColor(context, R.color.primary))
                when (item.type) {
                    "reminder" -> holder.imgType.setImageResource(R.drawable.ic_pill)
                    "refill" -> holder.imgType.setImageResource(R.drawable.ic_warning)
                    "guidance", "adherence", "health" -> holder.imgType.setImageResource(R.drawable.ic_info)
                    else -> holder.imgType.setImageResource(R.drawable.ic_bell)
                }
            }

            holder.txtMessage.text = item.message
            holder.txtTime.text = item.time
            holder.btnAction.text = item.actionLabel

            holder.btnAction.setOnClickListener {
                onActionClick(item)
            }

            holder.btnDismiss.setOnClickListener {
                onDismissClick(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}

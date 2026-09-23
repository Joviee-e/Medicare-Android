package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

object NotificationHelper {

    data class NotificationItem(
        val id: String,
        val title: String,
        val message: String,
        val time: String,
        val type: String, // "reminder", "refill", "health"
        val actionLabel: String = "Take Now"
    )

    // In-memory notification store for the active session
    val activeNotifications = mutableListOf(
        NotificationItem(
            id = "notif_1",
            title = "Morning Dose: Metformin 500mg",
            message = "Scheduled for 8:00 AM with food. Don't forget your morning dose.",
            time = "Today, 8:00 AM",
            type = "reminder",
            actionLabel = "Log Dose"
        ),
        NotificationItem(
            id = "notif_2",
            title = "Refill Alert: Atorvastatin 20mg",
            message = "Only 3 days of medication remaining in your current supply.",
            time = "Yesterday",
            type = "refill",
            actionLabel = "View Supply"
        ),
        NotificationItem(
            id = "notif_3",
            title = "Afternoon Dose: Aspirin 100mg",
            message = "Scheduled for 1:00 PM after lunch.",
            time = "Today, 1:00 PM",
            type = "reminder",
            actionLabel = "Log Dose"
        ),
        NotificationItem(
            id = "notif_4",
            title = "Daily Health Tip: Hydration",
            message = "Drink a full glass of water with your tablets to improve absorption.",
            time = "2 days ago",
            type = "health",
            actionLabel = "Got It"
        )
    )

    fun show(activity: FragmentActivity) {
        val sheet = NotificationBottomSheet()
        sheet.show(activity.supportFragmentManager, "NotificationBottomSheet")
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
                activeNotifications.clear()
                applyFilter()
            }

            applyFilter()
        }

        private fun applyFilter() {
            displayedItems.clear()
            if (currentFilter == "all") {
                displayedItems.addAll(activeNotifications)
            } else {
                displayedItems.addAll(activeNotifications.filter { it.type == currentFilter })
            }

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
                        val intent = Intent(requireContext(), MedicinesActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        startActivity(intent)
                    },
                    onDismissClick = { item ->
                        activeNotifications.removeAll { it.id == item.id }
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
            holder.txtTitle.text = item.title
            holder.txtMessage.text = item.message
            holder.txtTime.text = item.time
            holder.btnAction.text = item.actionLabel

            when (item.type) {
                "reminder" -> {
                    holder.imgType.setImageResource(R.drawable.ic_pill)
                }
                "refill" -> {
                    holder.imgType.setImageResource(R.drawable.ic_warning)
                }
                "health" -> {
                    holder.imgType.setImageResource(R.drawable.ic_info)
                }
                else -> {
                    holder.imgType.setImageResource(R.drawable.ic_bell)
                }
            }

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

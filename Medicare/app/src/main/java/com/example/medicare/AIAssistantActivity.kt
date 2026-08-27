package com.example.medicare

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AIAssistantActivity : BaseActivity() {

    private val chatData = mutableListOf<ChatItem>()
    private lateinit var adapter: ChatAdapter
    private lateinit var recyclerChat: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_ai)

        // Hide bottom navigation when keyboard is open to prevent visual compression/misalignment
        val rootView = findViewById<android.view.View>(android.R.id.content)
        val bottomNav = findViewById<android.view.View>(R.id.bottom_navigation)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > screenHeight * 0.15) {
                bottomNav.visibility = android.view.View.GONE
            } else {
                bottomNav.visibility = android.view.View.VISIBLE
            }
        }

        // Setup chat RecyclerView
        recyclerChat = findViewById(R.id.recycler_chat)
        recyclerChat.layoutManager = LinearLayoutManager(this)

        // Add initial conversation logs
        chatData.addAll(listOf(
            ChatItem("Hello! I'm your MediCare+ AI Assistant. How can I help you manage your health today?", isUser = false),
            ChatItem("", isUser = false, isSuggestions = true),
            ChatItem("What are some good low-sodium snacks I can have in the evening?", isUser = true, time = "10:42 AM"),
            ChatItem(
                "Here are some excellent low-sodium snack options for the evening that are gentle on your heart and easy to digest:\n\n• Unsalted mixed nuts: A small handful of almonds or walnuts.\n• Fresh fruit: Apple slices or a small bowl of berries.\n• Air-popped popcorn: Plain, without added butter or salt (you can use herbal seasoning).\n• Vegetable sticks: Carrots, celery, or cucumber with a small amount of low-sodium hummus.\n\nWould you like me to add any of these to your shopping list or schedule a reminder?",
                isUser = false
            )
        ))

        adapter = ChatAdapter(chatData) { suggestionText ->
            sendMessage(suggestionText)
        }
        recyclerChat.adapter = adapter

        // Send Button click action
        val inputMessage = findViewById<EditText>(R.id.input_message)
        findViewById<CardView>(R.id.btn_input_send)?.setOnClickListener {
            val text = inputMessage.text.toString()
            if (text.trim().isNotEmpty()) {
                sendMessage(text)
                inputMessage.setText("")
            }
        }

        // Voice Assistant
        findViewById<CardView>(R.id.btn_input_mic)?.setOnClickListener {
            Toast.makeText(this, "Voice assistant coming soon", Toast.LENGTH_SHORT).show()
        }

        // Plus attachment button
        findViewById<ImageView>(R.id.btn_input_add)?.setOnClickListener {
            Toast.makeText(this, "Attachment feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Chat settings overflow menu
        findViewById<ImageView>(R.id.btn_menu_dots)?.setOnClickListener {
            Toast.makeText(this, "Chat settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage(text: String) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = sdf.format(Date())

        // Add user message
        chatData.add(ChatItem(text, isUser = true, time = currentTime))
        adapter.notifyItemInserted(chatData.size - 1)
        recyclerChat.scrollToPosition(chatData.size - 1)

        // Delay AI reply simulation
        Handler(Looper.getMainLooper()).postDelayed({
            chatData.add(ChatItem("I understand. This is currently a placeholder AI response.", isUser = false))
            adapter.notifyItemInserted(chatData.size - 1)
            recyclerChat.scrollToPosition(chatData.size - 1)
        }, 800)
    }
}

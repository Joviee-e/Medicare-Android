package com.example.medicare

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.api.ChatRequest
import com.example.medicare.api.ChatResponse
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AIAssistantActivity : BaseActivity() {

    private val chatData = mutableListOf<ChatItem>()
    private lateinit var adapter: ChatAdapter
    private lateinit var recyclerChat: RecyclerView
    private var pendingContextMap: MutableMap<String, String>? = null

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

        // Check if launched with notification context or prompt
        val incomingPrompt = intent.getStringExtra("ai_prompt")
        val incomingTitle = intent.getStringExtra("context_title")
        val incomingMessage = intent.getStringExtra("context_message")
        val incomingType = intent.getStringExtra("context_type")

        if (incomingTitle != null && incomingMessage != null) {
            pendingContextMap = mutableMapOf(
                "title" to incomingTitle,
                "alert" to incomingMessage,
                "type" to (incomingType ?: "general")
            )
            val introText = "I found this medication finding on your account:\n\n• $incomingTitle\n$incomingMessage\n\nHow can I help you understand this finding or discuss precautions?"
            chatData.addAll(listOf(
                ChatItem(introText, isUser = false),
                ChatItem("", isUser = false, isSuggestions = true)
            ))
        } else {
            // Default welcoming messages
            chatData.addAll(listOf(
                ChatItem("Hello! I'm your MediCare+ AI Assistant. How can I help you manage your health and medications today?", isUser = false),
                ChatItem("", isUser = false, isSuggestions = true)
            ))
        }

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

        // If an explicit inquiry prompt was passed from a notification tap, send it immediately
        if (incomingPrompt != null && incomingTitle == null) {
            sendMessage(incomingPrompt)
        }
    }

    private fun sendMessage(text: String) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = sdf.format(Date())

        // 1. Add user message to UI
        chatData.add(ChatItem(text, isUser = true, time = currentTime))
        adapter.notifyItemInserted(chatData.size - 1)
        recyclerChat.scrollToPosition(chatData.size - 1)

        // 2. Add temporary loading indicator bubble
        val loadingIndex = chatData.size
        chatData.add(ChatItem("Consulting MediCare+ AI...", isUser = false))
        adapter.notifyItemInserted(loadingIndex)
        recyclerChat.scrollToPosition(loadingIndex)

        // 3. Make live backend API call to Flask -> Gemini
        val request = ChatRequest(
            message = text,
            context = pendingContextMap
        )

        RetrofitClient.getApiService(this).chatAi(request)
            .enqueue(object : Callback<ChatResponse> {
                override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                    val body = response.body()
                    val aiReply = if (response.isSuccessful && body != null && body.success && !body.reply.isNullOrEmpty()) {
                        body.reply
                    } else {
                        "AI explanation service is temporarily unavailable. Based on your records, your medication information remains securely stored. Please consult your physician or pharmacist."
                    }

                    // Clear pending context after first contextual query
                    pendingContextMap = null

                    // Replace loading bubble with real AI response
                    if (loadingIndex in chatData.indices) {
                        chatData[loadingIndex] = ChatItem(aiReply, isUser = false)
                        adapter.notifyItemChanged(loadingIndex)
                        recyclerChat.scrollToPosition(loadingIndex)
                    }
                }

                override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    val fallbackText = "Network connection error. Please verify your internet connection or check your medication label."
                    if (loadingIndex in chatData.indices) {
                        chatData[loadingIndex] = ChatItem(fallbackText, isUser = false)
                        adapter.notifyItemChanged(loadingIndex)
                        recyclerChat.scrollToPosition(loadingIndex)
                    }
                }
            })
    }
}

package com.example.medicare

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private var selectedMediaUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            findViewById<View>(R.id.layout_attachment_preview)?.visibility = View.VISIBLE
            findViewById<ImageView>(R.id.img_attachment_thumb)?.setImageURI(uri)
            val fileName = getFileName(uri) ?: "selected_image.jpg"
            findViewById<TextView>(R.id.txt_attachment_name)?.text = fileName
            Toast.makeText(this, "Media preview loaded", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_ai)

        // Notification Bell trigger in standardized header
        findViewById<ImageView>(R.id.btn_notification)?.setOnClickListener {
            NotificationHelper.show(this)
        }

        // Hide bottom navigation when keyboard is open to keep message composer directly above keyboard
        val rootView = findViewById<View>(android.R.id.content)
        val bottomNav = findViewById<View>(R.id.bottom_navigation)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            bottomNav.visibility = if (imeVisible) View.GONE else View.VISIBLE
            if (imeVisible && chatData.isNotEmpty()) {
                recyclerChat.post {
                    recyclerChat.scrollToPosition(chatData.size - 1)
                }
            }
            insets
        }

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > screenHeight * 0.15) {
                if (bottomNav.visibility != View.GONE) {
                    bottomNav.visibility = View.GONE
                    if (chatData.isNotEmpty()) {
                        recyclerChat.post {
                            recyclerChat.scrollToPosition(chatData.size - 1)
                        }
                    }
                }
            } else {
                if (bottomNav.visibility != View.VISIBLE) {
                    bottomNav.visibility = View.VISIBLE
                }
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

        val promptToSend = when {
            !incomingPrompt.isNullOrBlank() -> incomingPrompt
            incomingTitle != null && incomingMessage != null ->
                "Can you explain this finding: \"$incomingTitle\" ($incomingMessage), and what precautions or adherence steps I should follow?"
            else -> null
        }

        if (promptToSend != null) {
            if (incomingTitle != null && incomingMessage != null) {
                pendingContextMap = mutableMapOf(
                    "title" to incomingTitle,
                    "alert" to incomingMessage,
                    "type" to (incomingType ?: "general")
                )
            }
            chatData.add(ChatItem("Hello! I'm your MediCare+ AI Assistant. Reviewing your medication context...", isUser = false))
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

        // Media attachment cancel button
        val previewBar = findViewById<View>(R.id.layout_attachment_preview)
        findViewById<ImageView>(R.id.btn_cancel_attachment)?.setOnClickListener {
            selectedMediaUri = null
            previewBar?.visibility = View.GONE
        }

        // Attachment media picker button
        findViewById<ImageView>(R.id.btn_input_add)?.setOnClickListener {
            try {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open system media picker", Toast.LENGTH_SHORT).show()
            }
        }

        // Send Button click action
        val inputMessage = findViewById<EditText>(R.id.input_message)
        findViewById<CardView>(R.id.btn_input_send)?.setOnClickListener {
            if (selectedMediaUri != null) {
                Toast.makeText(
                    this,
                    "Media analysis is not supported by the AI backend yet. Please ask text questions or cancel the photo attachment.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val text = inputMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                inputMessage.setText("")
            }
        }

        // If an explicit inquiry prompt or finding was passed, automatically dispatch the explanation inquiry
        if (promptToSend != null) {
            recyclerChat.post {
                sendMessage(promptToSend)
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return name ?: uri.lastPathSegment
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

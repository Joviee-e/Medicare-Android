package com.example.medicare

data class ChatItem(
    val text: String,
    val isUser: Boolean,
    val time: String = "",
    val isSuggestions: Boolean = false
)

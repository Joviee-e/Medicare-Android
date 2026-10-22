package com.example.medicare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val items: List<ChatItem>,
    private val onSuggestionClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SUGGESTIONS = 0
        private const val TYPE_USER = 1
        private const val TYPE_ASSISTANT = 2
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return if (item.isSuggestions) {
            TYPE_SUGGESTIONS
        } else if (item.isUser) {
            TYPE_USER
        } else {
            TYPE_ASSISTANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SUGGESTIONS -> {
                val view = inflater.inflate(R.layout.item_chat_suggestions, parent, false)
                SuggestionsViewHolder(view)
            }
            TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_chat_assistant, parent, false)
                AssistantViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is UserViewHolder -> {
                holder.txtMessage.text = item.text
                holder.txtTime.text = item.time
            }
            is AssistantViewHolder -> {
                holder.txtMessage.text = item.text
            }
            is SuggestionsViewHolder -> {
                holder.chipRemind.setOnClickListener { onSuggestionClick("Remind me about my pills") }
                holder.chipFood.setOnClickListener { onSuggestionClick("Healthy food suggestions") }
                holder.chipEffects.setOnClickListener { onSuggestionClick("Side Effects") }
                holder.chipSodium.setOnClickListener { onSuggestionClick("Low Sodium Diet") }
            }
        }
    }

    override fun getItemCount() = items.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txt_chat_message)
        val txtTime: TextView = view.findViewById(R.id.txt_chat_time)
    }

    class AssistantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txt_chat_message)
    }

    class SuggestionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chipRemind: View = view.findViewById(R.id.chip_suggest_remind)
        val chipFood: View = view.findViewById(R.id.chip_suggest_food)
        val chipEffects: View = view.findViewById(R.id.chip_suggest_effects)
        val chipSodium: View = view.findViewById(R.id.chip_suggest_sodium)
    }
}

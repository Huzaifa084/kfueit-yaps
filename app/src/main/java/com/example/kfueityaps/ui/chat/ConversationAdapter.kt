package com.example.kfueityaps.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.kfueityaps.databinding.ItemConversationBinding
import com.example.kfueityaps.data.model.Conversation
import com.example.kfueityaps.R

class ConversationAdapter(
    private val currentUserId: String,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    private val conversations = mutableListOf<Conversation>()

    fun submitList(newConversations: List<Conversation>) {
        conversations.clear()
        conversations.addAll(newConversations)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    inner class ViewHolder(private val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(conversation: Conversation) {
            val otherUser = if (conversation.user1_id == currentUserId) {
                conversation.user2_profile
            } else {
                conversation.user1_profile
            }

            binding.tvName.text = otherUser?.full_name ?: "Unknown User"
            binding.tvLastMessage.text = conversation.last_message ?: "No messages yet"
            binding.ivAvatar.load(otherUser?.avatar_url ?: R.drawable.ic_launcher_foreground) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
            }

            binding.root.setOnClickListener {
                onConversationClick(conversation)
            }
        }
    }
}

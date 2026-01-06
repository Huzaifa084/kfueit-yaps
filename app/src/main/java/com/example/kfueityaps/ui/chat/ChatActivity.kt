package com.example.kfueityaps.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.kfueityaps.databinding.ActivityChatBinding
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.model.Message
import com.example.kfueityaps.data.model.Conversation
import com.example.kfueityaps.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private var conversationId: String? = null
    private var otherUserId: String? = null
    private var otherUserName: String? = null
    private var otherUserAvatar: String? = null
    private val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationId = intent.getStringExtra("CONVERSATION_ID")
        otherUserId = intent.getStringExtra("OTHER_USER_ID")
        otherUserName = intent.getStringExtra("OTHER_USER_NAME")
        otherUserAvatar = intent.getStringExtra("OTHER_USER_AVATAR")

        setupUI()
        setupAdapter()
        
        if (conversationId != null) {
            fetchMessages()
            setupRealtime()
            checkConversationStatus()
        } else if (otherUserId != null) {
            // New chat request or existing check
            checkExistingConversation()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnAccept.setOnClickListener {
            updateConversationStatus("ACCEPTED")
        }

        binding.btnDecline.setOnClickListener {
            finish() // Or add ignore logic
        }
    }

    private fun setupUI() {
        binding.tvToolbarName.text = otherUserName ?: "Chat"
        binding.ivToolbarAvatar.load(otherUserAvatar ?: R.drawable.ic_launcher_foreground) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_foreground)
        }
    }

    private fun setupAdapter() {
        adapter = ChatAdapter(currentUserId, otherUserAvatar)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun checkExistingConversation() {
        lifecycleScope.launch {
            try {
                val conversation = SupabaseConfig.client.postgrest["conversations"]
                    .select {
                        filter {
                            or {
                                and {
                                    eq("user1_id", currentUserId)
                                    eq("user2_id", otherUserId!!)
                                }
                                and {
                                    eq("user1_id", otherUserId!!)
                                    eq("user2_id", currentUserId)
                                }
                            }
                        }
                    }.decodeSingleOrNull<Conversation>()

                if (conversation != null) {
                    conversationId = conversation.id
                    fetchMessages()
                    setupRealtime()
                    checkConversationStatus()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchMessages() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val messages = SupabaseConfig.client.postgrest["messages"]
                    .select {
                        filter {
                            eq("conversation_id", conversationId!!)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }.decodeList<Message>()
                
                adapter.submitList(messages)
                binding.rvMessages.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ChatActivity, "Error loading messages", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupRealtime() {
        val channel = SupabaseConfig.client.channel("messages_channel")
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }
        
        flow.onEach { action ->
            // Manual filter since the internal filter property is private
            val json = Json { ignoreUnknownKeys = true }
            val newMessage = json.decodeFromJsonElement<Message>(action.record)
            if (newMessage.conversation_id == conversationId) {
                runOnUiThread {
                    adapter.addMessage(newMessage)
                    binding.rvMessages.smoothScrollToPosition(adapter.itemCount - 1)
                }
            }
        }.launchIn(lifecycleScope)

        lifecycleScope.launch {
            channel.subscribe()
        }
    }

    private fun checkConversationStatus() {
        lifecycleScope.launch {
            try {
                val conversation = SupabaseConfig.client.postgrest["conversations"]
                    .select {
                        filter { eq("id", conversationId!!) }
                    }.decodeSingle<Conversation>()

                if (conversation.status == "REQUESTED" && conversation.user2_id == currentUserId) {
                    binding.layoutRequestBar.visibility = View.VISIBLE
                } else {
                    binding.layoutRequestBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateConversationStatus(status: String) {
        lifecycleScope.launch {
            try {
                SupabaseConfig.client.postgrest["conversations"].update({
                    Conversation::status setTo status
                }) {
                    filter { eq("id", conversationId!!) }
                }
                binding.layoutRequestBar.visibility = View.GONE
                Toast.makeText(this@ChatActivity, "Chat accepted!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ChatActivity, "Error updating status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage() {
        val content = binding.etMessage.text.toString().trim()
        if (content.isEmpty()) return

        lifecycleScope.launch {
            try {
                if (conversationId == null) {
                    // Create new conversation as request
                    val newConv = Conversation(
                        user1_id = currentUserId,
                        user2_id = otherUserId!!,
                        status = "REQUESTED",
                        last_message = content
                    )
                    val insertedConv = SupabaseConfig.client.postgrest["conversations"]
                        .insert(newConv) { select() }
                        .decodeSingle<Conversation>()
                    
                    conversationId = insertedConv.id
                    setupRealtime()
                }

                val message = Message(
                    conversation_id = conversationId!!,
                    sender_id = currentUserId,
                    content = content
                )
                
                SupabaseConfig.client.postgrest["messages"].insert(message)
                
                // Update last message in conversation
                SupabaseConfig.client.postgrest["conversations"].update({
                    Conversation::last_message setTo content
                }) {
                    filter { eq("id", conversationId!!) }
                }

                binding.etMessage.text.clear()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ChatActivity, "Error sending message", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

package com.example.kfueityaps.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kfueityaps.databinding.ActivityChatListBinding
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.model.Conversation
import com.example.kfueityaps.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var adapter: ConversationAdapter
    private val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""
    private var currentTab = "ACCEPTED" // "ACCEPTED" for Messages, "REQUESTED" for Requests

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapter()
        fetchConversations()

        binding.tvTabMessages.setOnClickListener {
            switchTab("ACCEPTED")
        }

        binding.tvTabRequests.setOnClickListener {
            switchTab("REQUESTED")
        }

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupAdapter() {
        adapter = ConversationAdapter(currentUserId) { conversation ->
            val otherUser = if (conversation.user1_id == currentUserId) {
                conversation.user2_profile
            } else {
                conversation.user1_profile
            }

            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("CONVERSATION_ID", conversation.id)
                putExtra("OTHER_USER_ID", otherUser?.id)
                putExtra("OTHER_USER_NAME", otherUser?.full_name)
                putExtra("OTHER_USER_AVATAR", otherUser?.avatar_url)
            }
            startActivity(intent)
        }
        binding.rvConversations.layoutManager = LinearLayoutManager(this)
        binding.rvConversations.adapter = adapter
    }

    private fun switchTab(tab: String) {
        if (currentTab == tab) return
        currentTab = tab
        
        // UI updates for tabs
        if (tab == "ACCEPTED") {
            binding.tvTabMessages.setTextColor(getColor(R.color.text_primary))
            binding.tvTabMessages.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvTabRequests.setTextColor(getColor(R.color.text_secondary))
            binding.tvTabRequests.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.tabIndicator.animate().translationX(0f).setDuration(200).start()
        } else {
            binding.tvTabMessages.setTextColor(getColor(R.color.text_secondary))
            binding.tvTabMessages.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.tvTabRequests.setTextColor(getColor(R.color.text_primary))
            binding.tvTabRequests.setTypeface(null, android.graphics.Typeface.BOLD)
            val translationX = binding.tvTabRequests.x - binding.tvTabMessages.x
            binding.tabIndicator.animate().translationX(translationX).setDuration(200).start()
        }
        
        fetchConversations()
    }

    private fun fetchConversations() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                
                // Fetch conversations with profile data
                val conversations = SupabaseConfig.client.postgrest["conversations"]
                    .select(Columns.raw("*, user1_profile:profiles!user1_id(*), user2_profile:profiles!user2_id(*)")) {
                        filter {
                            and {
                                or {
                                    eq("user1_id", currentUserId)
                                    eq("user2_id", currentUserId)
                                }
                                eq("status", currentTab)
                            }
                        }
                        order("updated_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }.decodeList<Conversation>()

                adapter.submitList(conversations)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ChatListActivity, "Error loading conversations", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}

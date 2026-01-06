package com.example.kfueityaps.ui.post

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kfueityaps.databinding.ActivityCommentsBinding
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.model.Comment
import com.example.kfueityaps.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class CommentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding
    private var postId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getLongExtra("POST_ID", -1)
        if (postId == -1L) {
            finish()
            return
        }

        binding.ivBack.setOnClickListener { finish() }

        binding.rvComments.layoutManager = LinearLayoutManager(this)

        binding.btnSendComment.setOnClickListener {
            sendComment()
        }

        fetchComments()
    }

    private fun fetchComments() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val comments = SupabaseConfig.client.postgrest["comments"]
                    .select(Columns.raw("*, profiles(*)")) {
                        filter {
                            eq("post_id", postId)
                        }
                    }.decodeList<Comment>()
                    .sortedBy { it.created_at }

                binding.rvComments.adapter = CommentsAdapter(comments)
            } catch (e: Exception) {
                Toast.makeText(this@CommentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun sendComment() {
        val content = binding.etComment.text.toString().trim()
        val user = SupabaseConfig.client.auth.currentUserOrNull()

        if (user == null || content.isEmpty()) return

        lifecycleScope.launch {
            try {
                binding.btnSendComment.isEnabled = false
                val comment = Comment(post_id = postId, user_id = user.id, content = content)
                SupabaseConfig.client.postgrest["comments"].insert(comment)
                
                binding.etComment.text.clear()
                fetchComments()
            } catch (e: Exception) {
                Toast.makeText(this@CommentsActivity, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSendComment.isEnabled = true
            }
        }
    }
}

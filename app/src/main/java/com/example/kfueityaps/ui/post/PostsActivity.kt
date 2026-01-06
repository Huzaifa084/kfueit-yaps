package com.example.kfueityaps.ui.post

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kfueityaps.BuildConfig
import com.example.kfueityaps.databinding.ActivityPostsBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.model.Post
import com.example.kfueityaps.data.model.Like
import com.example.kfueityaps.data.model.Comment
import com.example.kfueityaps.data.model.Profile
import com.example.kfueityaps.ui.chat.ChatListActivity
import com.example.kfueityaps.R
import com.example.kfueityaps.ui.ads.AdaptiveBanner
import com.google.android.gms.ads.AdView



class PostsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostsBinding
    private var adapter: PostsAdapter? = null
    private var category: String = ""
    private var bannerAdView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (BuildConfig.ADMOB_BANNER_AD_UNIT_ID.isNotBlank()) {
            bannerAdView = AdaptiveBanner.load(
                activity = this,
                container = binding.adContainer,
                adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID,
            )
        } else {
            binding.adContainer.visibility = View.GONE
        }

        category = intent.getStringExtra("CATEGORY") ?: ""
        binding.tvCategoryTitle.text = category

        setupCategoryUI()

        binding.ivBack.setOnClickListener { finish() }

        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        binding.fabAddPost.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            intent.putExtra("CATEGORY", category)
            startActivity(intent)
        }

        binding.btnChatList.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }
    }

    private fun setupCategoryUI() {
        val colorRes = when (category) {
            "Random" -> R.color.cat_confessions
            "Questions" -> R.color.cat_questions
            "Advice" -> R.color.cat_advice
            "Stories" -> R.color.cat_stories
            "General" -> R.color.cat_random
            else -> R.color.primary_dark
        }
        
        val color = getColor(colorRes)
        binding.topBar.setBackgroundColor(color)
        binding.fabAddPost.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        window.statusBarColor = color
    }

    override fun onResume() {
        super.onResume()
        bannerAdView?.resume()
        // If adapter is null, fresh fetch. If not, we might want to refresh anyway but carefully?
        // Usually, onResume refresh is fine but might jump. Let's only fetch if null for now
        // or just let it replace once on resume.
        fetchPosts()
    }

    override fun onPause() {
        bannerAdView?.pause()
        super.onPause()
    }

    private fun fetchPosts() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val posts = SupabaseConfig.client.postgrest["posts"]
                    .select(Columns.raw("*, profiles(*), likes(*), comments(*)")) {
                        filter {
                            eq("category", category)
                        }
                    }.decodeList<Post>()
                    .sortedByDescending { it.created_at }

                if (adapter == null) {
                    adapter = PostsAdapter(posts.toMutableList()) { postId ->
                        fetchSinglePost(postId)
                    }
                    binding.rvPosts.adapter = adapter
                } else {
                    // For a full refresh (like onResume), we can still update the list 
                    // without recreating the adapter to preserve scroll position if possible.
                    // But if it's a completely different set, replacing is safer.
                    // For now, let's keep it simple: if it's an action, we call fetchSinglePost.
                    adapter = PostsAdapter(posts.toMutableList()) { postId ->
                        fetchSinglePost(postId)
                    }
                    binding.rvPosts.adapter = adapter
                }
            } catch (e: Exception) {
                Toast.makeText(this@PostsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun fetchSinglePost(postId: Long) {
        lifecycleScope.launch {
            try {
                val updatedPost = SupabaseConfig.client.postgrest["posts"]
                    .select(Columns.raw("*, profiles(*), likes(*), comments(*)")) {
                        filter {
                            eq("id", postId)
                        }
                    }.decodeSingle<Post>()
                
                adapter?.updatePost(updatedPost)
            } catch (e: Exception) {
                println("Update Error: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        super.onDestroy()
    }
}

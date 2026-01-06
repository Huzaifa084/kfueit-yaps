package com.example.kfueityaps.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kfueityaps.databinding.ActivityMainBinding
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.ui.auth.LoginActivity
import com.example.kfueityaps.ui.profile.ProfileActivity
import com.example.kfueityaps.ui.post.PostsActivity
import com.example.kfueityaps.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivLogout.setOnClickListener {
            lifecycleScope.launch {
                SupabaseConfig.client.auth.signOut()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
        
        SupabaseConfig.client.handleDeeplinks(intent) {
            // Session verified
            android.widget.Toast.makeText(this@MainActivity, "Email verified!", android.widget.Toast.LENGTH_SHORT).show()
        }

        binding.btnGoToProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        setupCategoryClick(binding.cardConfessions, "Random")
        setupCategoryClick(binding.cardQuestions, "Questions")
        setupCategoryClick(binding.cardAdvice, "Advice")
        setupCategoryClick(binding.cardStories, "Stories")
        setupCategoryClick(binding.cardRandom, "General")
    }

    private fun setupCategoryClick(view: android.view.View, category: String) {
        view.setOnClickListener {
            val intent = Intent(this, PostsActivity::class.java)
            intent.putExtra("CATEGORY", category)
            startActivity(intent)
        }
    }
}

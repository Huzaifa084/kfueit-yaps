package com.example.kfueityaps.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kfueityaps.BuildConfig
import com.example.kfueityaps.databinding.ActivityMainBinding
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.prefs.TocAcceptanceStore
import com.example.kfueityaps.ui.ads.AdaptiveBanner
import com.example.kfueityaps.ui.auth.LoginActivity
import com.example.kfueityaps.ui.legal.TermsAndConditionsActivity
import com.example.kfueityaps.ui.profile.ProfileActivity
import com.example.kfueityaps.ui.post.PostsActivity
import com.example.kfueityaps.R
import com.google.android.gms.ads.AdView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var bannerAdView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!TocAcceptanceStore.isAccepted(this)) {
            startActivity(
                Intent(this, TermsAndConditionsActivity::class.java)
                    .putExtra(TermsAndConditionsActivity.EXTRA_RESUME_INTENT, intent)
            )
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
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

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        bannerAdView?.resume()
    }

    override fun onPause() {
        bannerAdView?.pause()
        super.onPause()
    }
}

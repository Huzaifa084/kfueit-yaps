package com.example.kfueityaps.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.ui.auth.LoginActivity
import com.example.kfueityaps.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animation
        val logo = findViewById<android.view.View>(R.id.ivLogo)
        val title = findViewById<android.view.View>(R.id.tvAppName)
        
        logo.alpha = 0f
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        title.alpha = 0f
        title.translationY = 50f

        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(800).setStartDelay(200).start()
        title.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(400).start()

        lifecycleScope.launch {
            delay(2500) // Allow animation to finish + read time
            val session = SupabaseConfig.client.auth.currentSessionOrNull()
            if (session != null) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            // Fade out transition
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

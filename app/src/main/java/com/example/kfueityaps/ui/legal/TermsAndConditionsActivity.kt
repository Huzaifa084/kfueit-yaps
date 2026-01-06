package com.example.kfueityaps.ui.legal

import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kfueityaps.R
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.prefs.TocAcceptanceStore
import com.example.kfueityaps.databinding.ActivityTermsAndConditionsBinding
import com.example.kfueityaps.ui.auth.LoginActivity
import com.example.kfueityaps.ui.main.MainActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class TermsAndConditionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTermsAndConditionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTermsAndConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvTermsBody.text = readTermsText()

        binding.btnAccept.isEnabled = false
        binding.btnAccept.alpha = 0.55f

        binding.cbAgree.setOnCheckedChangeListener { _, isChecked ->
            binding.btnAccept.isEnabled = isChecked
            binding.btnAccept.alpha = if (isChecked) 1f else 0.55f
        }

        binding.btnDecline.setOnClickListener {
            finishAffinity()
        }

        binding.btnAccept.setOnClickListener {
            lifecycleScope.launch {
                TocAcceptanceStore.setAccepted(this@TermsAndConditionsActivity, true)

                val resumeIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESUME_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESUME_INTENT)
                }
                if (resumeIntent != null) {
                    startActivity(resumeIntent)
                } else {
                    val session = SupabaseConfig.client.auth.currentSessionOrNull()
                    if (session != null) {
                        startActivity(Intent(this@TermsAndConditionsActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@TermsAndConditionsActivity, LoginActivity::class.java))
                    }
                }

                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    private fun readTermsText(): String {
        return resources.openRawResource(R.raw.terms_and_conditions)
            .bufferedReader()
            .use { it.readText() }
    }

    companion object {
        const val EXTRA_RESUME_INTENT = "extra_resume_intent"
    }
}

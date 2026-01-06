package com.example.kfueityaps.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kfueityaps.databinding.ActivityLoginBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.ui.main.MainActivity
import com.example.kfueityaps.data.model.Profile
import com.example.kfueityaps.R

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        lifecycleScope.launch {
            try {
                val session = SupabaseConfig.client.auth.currentSessionOrNull()
                if (session != null) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                // Ignore session check errors on startup
            }
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    binding.progressBar.visibility = View.VISIBLE
                    SupabaseConfig.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    // Self-healing: Check if profile exists, if not create it
                    val user = SupabaseConfig.client.auth.currentUserOrNull()
                    if (user != null) {
                        val profile = SupabaseConfig.client.postgrest["profiles"].select {
                            filter { eq("id", user.id) }
                        }.decodeSingleOrNull<Profile>()

                        if (profile == null) {
                            val newProfile = Profile(
                                id = user.id,
                                email = user.email ?: "",
                                full_name = "User ${user.email?.substringBefore("@") ?: ""}"
                            )
                            SupabaseConfig.client.postgrest["profiles"].insert(newProfile)
                        }
                    }

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
}

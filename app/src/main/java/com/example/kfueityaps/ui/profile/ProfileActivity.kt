package com.example.kfueityaps.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.model.Profile
import com.example.kfueityaps.databinding.ActivityProfileBinding
import com.example.kfueityaps.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var selectedImageUri: Uri? = null

    private val getAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            startCrop(it)
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationFileName = "avatar_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(java.io.File(cacheDir, destinationFileName))
        
        com.yalantis.ucrop.UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(500, 500)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == com.yalantis.ucrop.UCrop.REQUEST_CROP) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(data!!)
            resultUri?.let { uploadAvatar(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }

        loadProfile()

        binding.fabEditAvatar.setOnClickListener {
            getAvatar.launch("image/*")
        }

        binding.btnEditName.setOnClickListener {
            showEditNameDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun loadProfile() {
        val client = SupabaseConfig.client
        val user = client.auth.currentUserOrNull()

        if (user != null) {
            binding.tvProfileEmail.text = user.email

            lifecycleScope.launch {
                try {
                    val profile = client.postgrest["profiles"]
                        .select {
                            filter { eq("id", user.id) }
                        }.decodeSingle<Profile>()
                    
                    binding.tvProfileName.text = profile.full_name
                    
                    if (!profile.avatar_url.isNullOrEmpty()) {
                        binding.ivAvatar.load(profile.avatar_url) {
                            crossfade(true)
                            transformations(CircleCropTransformation())
                            placeholder(android.R.drawable.ic_menu_report_image)
                        }
                    }
                } catch (e: Exception) {
                    binding.tvProfileName.text = "User"
                }
            }
        } else {
            finish()
        }
    }

    private fun showEditNameDialog() {
        val editText = EditText(this)
        editText.setText(binding.tvProfileName.text)
        editText.hint = "Your Full Name"

        AlertDialog.Builder(this)
            .setTitle("Edit Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateName(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateName(newName: String) {
        val user = SupabaseConfig.client.auth.currentUserOrNull() ?: return
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                SupabaseConfig.client.postgrest["profiles"].update(
                    mapOf("full_name" to newName)
                ) {
                    filter { eq("id", user.id) }
                }
                binding.tvProfileName.text = newName
                Toast.makeText(this@ProfileActivity, "Name updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val user = SupabaseConfig.client.auth.currentUserOrNull() ?: return
        
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val fileName = "avatars/${user.id}_${UUID.randomUUID()}.jpg"
                val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                
                val bucket = SupabaseConfig.client.storage.from("media")
                bucket.upload(fileName, bytes) { upsert = true }
                val publicUrl = bucket.publicUrl(fileName)

                SupabaseConfig.client.postgrest["profiles"].update(
                    mapOf("avatar_url" to publicUrl)
                ) {
                    filter { eq("id", user.id) }
                }

                binding.ivAvatar.load(publicUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                Toast.makeText(this@ProfileActivity, "Profile picture updated!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showChangePasswordDialog() {
        // (Existing change password logic remains same)
        val editText = EditText(this)
        editText.hint = "New Password"
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val newPassword = editText.text.toString()
                if (newPassword.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                } else {
                    updatePassword(newPassword)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePassword(password: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                SupabaseConfig.client.auth.updateUser {
                    this.password = password
                }
                Toast.makeText(this@ProfileActivity, "Password updated successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}

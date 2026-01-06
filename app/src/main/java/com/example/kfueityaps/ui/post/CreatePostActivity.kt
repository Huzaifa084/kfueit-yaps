package com.example.kfueityaps.ui.post

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kfueityaps.databinding.ActivityCreatePostBinding
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.model.Post
import com.example.kfueityaps.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private var selectedMediaUri: Uri? = null
    private var selectedDocUri: Uri? = null
    private var selectedDocName: String? = null
    private var category: String = ""
    private var isEditMode = false
    private var postId: Long = -1
    private var existingMediaUrls: List<String>? = null
    private var existingDocUrl: String? = null
    private var existingDocName: String? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (checkFileSize(it)) {
                startCrop(it)
            }
        }
    }

    private val getDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            if (checkFileSize(it)) {
                selectedDocUri = it
                selectedDocName = getFileName(it)
                binding.cvDocPreview.visibility = View.VISIBLE
                binding.tvDocName.text = selectedDocName
                
                // Clear image if doc selected
                selectedMediaUri = null
                binding.cvMediaPreview.visibility = View.GONE
            }
        }
    }

    private fun checkFileSize(uri: Uri): Boolean {
        val cursor = contentResolver.query(uri, null, null, null, null)
        val sizeIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE)
        cursor?.moveToFirst()
        val size = sizeIndex?.let { index -> cursor.getLong(index) } ?: 0
        cursor?.close()

        return if (size > 2 * 1024 * 1024) {
            Toast.makeText(this, "File size must be less than 2MB", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "document"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun startCrop(uri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(java.io.File(cacheDir, destinationFileName))
        
        com.yalantis.ucrop.UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1080, 1080)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == com.yalantis.ucrop.UCrop.REQUEST_CROP) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(data!!)
            resultUri?.let {
                selectedMediaUri = it
                binding.cvMediaPreview.visibility = View.VISIBLE
                binding.ivSelectedMedia.setImageURI(it)
                
                // Clear doc if image picked?
                selectedDocUri = null
                binding.cvDocPreview.visibility = View.GONE
            }
        } else if (resultCode == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
            val cropError = com.yalantis.ucrop.UCrop.getError(data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = intent.getStringExtra("CATEGORY") ?: ""
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        
        if (isEditMode) {
            postId = intent.getLongExtra("POST_ID", -1)
            val existingContent = intent.getStringExtra("POST_CONTENT")
            category = intent.getStringExtra("POST_CATEGORY") ?: category
            existingMediaUrls = intent.getStringArrayExtra("POST_MEDIA")?.toList()
            existingDocUrl = intent.getStringExtra("POST_DOC_URL")
            existingDocName = intent.getStringExtra("POST_DOC_NAME")
            
            binding.etPostContent.setText(existingContent)
            binding.btnSubmitPost.text = "Update Post"
            binding.tvTitle.text = "Edit Post"
            
            // Show existing media preview if any
            if (!existingMediaUrls.isNullOrEmpty()) {
                binding.cvMediaPreview.visibility = View.VISIBLE
                // Note: We can't easily show uri here without downloading, but we can placeholder it
                binding.ivSelectedMedia.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            // Show existing doc preview if any
            if (!existingDocUrl.isNullOrEmpty()) {
                binding.cvDocPreview.visibility = View.VISIBLE
                binding.tvDocName.text = existingDocName ?: "Existing Document"
                selectedDocName = existingDocName
            }
        }

        setupCategoryUI()

        binding.ivBack.setOnClickListener { finish() }

        binding.btnAttachMedia.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.btnAttachDoc.setOnClickListener {
            getDoc.launch(arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
        }

        binding.btnRemoveMedia.setOnClickListener {
            selectedMediaUri = null
            binding.cvMediaPreview.visibility = View.GONE
        }

        binding.btnRemoveDoc.setOnClickListener {
            selectedDocUri = null
            selectedDocName = null
            binding.cvDocPreview.visibility = View.GONE
        }

        binding.btnSubmitPost.setOnClickListener {
            submitPost()
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
        window.statusBarColor = color
    }

    private fun submitPost() {
        val content = binding.etPostContent.text.toString().trim()
        val user = SupabaseConfig.client.auth.currentUserOrNull()

        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (content.isEmpty() && selectedMediaUri == null && selectedDocUri == null) {
            Toast.makeText(this, "Post cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnSubmitPost.isEnabled = false

                val mediaUrls = mutableListOf<String>()
                var docUrl: String? = null

                // Upload Media if selected
                selectedMediaUri?.let { uri ->
                    val fileName = "posts/${UUID.randomUUID()}.jpg"
                    val bytes = contentResolver.openInputStream(uri)?.readBytes()
                    bytes?.let {
                        val bucket = SupabaseConfig.client.storage.from("media")
                        bucket.upload(fileName, it)
                        val publicUrl = bucket.publicUrl(fileName)
                        mediaUrls.add(publicUrl)
                    }
                }

                // Upload Document if selected
                selectedDocUri?.let { uri ->
                    val fileName = "docs/${UUID.randomUUID()}_${selectedDocName}"
                    val bytes = contentResolver.openInputStream(uri)?.readBytes()
                    bytes?.let {
                        val bucket = SupabaseConfig.client.storage.from("media") // Use same bucket for simplicity or separate
                        bucket.upload(fileName, it)
                        docUrl = bucket.publicUrl(fileName)
                    }
                }

                val post = Post(
                    user_id = user.id,
                    category = category,
                    content = content,
                    media_urls = if (selectedMediaUri != null) mediaUrls else existingMediaUrls,
                    document_url = if (selectedDocUri != null) docUrl else existingDocUrl,
                    document_name = if (selectedDocUri != null) selectedDocName else existingDocName
                )

                if (isEditMode) {
                    SupabaseConfig.client.postgrest["posts"].update(post) {
                        filter { eq("id", postId) }
                    }
                    Toast.makeText(this@CreatePostActivity, "Updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    SupabaseConfig.client.postgrest["posts"].insert(post)
                    Toast.makeText(this@CreatePostActivity, "Posted successfully!", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@CreatePostActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                println("Post Error: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitPost.isEnabled = true
            }
        }
    }
}

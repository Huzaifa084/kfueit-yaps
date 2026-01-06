package com.example.kfueityaps.ui.post

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.kfueityaps.databinding.ItemPostBinding
import com.example.kfueityaps.data.SupabaseConfig
import com.example.kfueityaps.data.model.Post
import com.example.kfueityaps.data.model.Like
import com.example.kfueityaps.ui.chat.ChatActivity
import com.example.kfueityaps.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostsAdapter(
    private val posts: MutableList<Post>,
    private val onAction: (Long) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    fun updatePost(updatedPost: Post) {
        val index = posts.indexOfFirst { it.id == updatedPost.id }
        if (index != -1) {
            posts[index] = updatedPost
            notifyItemChanged(index)
        }
    }

    class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val context = holder.itemView.context
        val currentUser = SupabaseConfig.client.auth.currentUserOrNull()
        val currentUserId = currentUser?.id

        // Fix Background Color
        holder.binding.root.setCardBackgroundColor(context.getColor(android.R.color.white))

        // Author Info
        holder.binding.tvAuthorName.text = post.profiles?.full_name ?: "Anonymous"
        holder.binding.ivAuthorAvatar.load(post.profiles?.avatar_url) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_report_image)
            transformations(CircleCropTransformation())
        }

        // Show Menu
        holder.binding.btnPostMenu.visibility = View.VISIBLE
        holder.binding.btnPostMenu.setOnClickListener { view ->
            val popup = PopupMenu(context, view)
            if (post.user_id == currentUserId) {
                popup.menu.add("Edit")
                popup.menu.add("Delete")
            } else {
                popup.menu.add("Message")
            }
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Edit" -> {
                        val intent = Intent(context, CreatePostActivity::class.java).apply {
                            putExtra("EDIT_MODE", true)
                            putExtra("POST_ID", post.id)
                            putExtra("POST_CONTENT", post.content)
                            putExtra("POST_CATEGORY", post.category)
                            putExtra("POST_MEDIA", post.media_urls?.toTypedArray())
                            putExtra("POST_DOC_URL", post.document_url)
                            putExtra("POST_DOC_NAME", post.document_name)
                        }
                        context.startActivity(intent)
                        true
                    }
                    "Delete" -> {
                        showDeleteConfirmation(context, post.id!!)
                        true
                    }
                    "Message" -> {
                        val intent = Intent(context, ChatActivity::class.java).apply {
                            putExtra("OTHER_USER_ID", post.user_id)
                            putExtra("OTHER_USER_NAME", post.profiles?.full_name ?: "User")
                            putExtra("OTHER_USER_AVATAR", post.profiles?.avatar_url)
                        }
                        context.startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Content & Media Logic
        val hasImage = !post.media_urls.isNullOrEmpty()
        val hasDoc = !post.document_url.isNullOrEmpty()
        val hasMediaOrDoc = hasImage || hasDoc

        if (hasMediaOrDoc) {
            // Display text as a caption
            holder.binding.tvMainContent.visibility = View.GONE
            holder.binding.tvContent.visibility = if (!post.content.isNullOrEmpty()) View.VISIBLE else View.GONE
            holder.binding.tvContent.text = post.content
            
            // Show Image if present
            if (hasImage) {
                holder.binding.ivPostMedia.visibility = View.VISIBLE
                holder.binding.pbImageLoader.visibility = View.VISIBLE
                holder.binding.ivPostMedia.load(post.media_urls!!.first()) {
                    crossfade(true)
                    listener(
                        onStart = { holder.binding.pbImageLoader.visibility = View.VISIBLE },
                        onSuccess = { _, _ -> holder.binding.pbImageLoader.visibility = View.GONE },
                        onError = { _, _ -> holder.binding.pbImageLoader.visibility = View.GONE }
                    )
                }
            } else {
                holder.binding.ivPostMedia.visibility = View.GONE
            }

            // Show Doc if present
            if (hasDoc) {
                holder.binding.layoutDocument.visibility = View.VISIBLE
                holder.binding.tvPostDocName.text = post.document_name ?: "Document"
                holder.binding.layoutDocument.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(post.document_url))
                    context.startActivity(intent)
                }
            } else {
                holder.binding.layoutDocument.visibility = View.GONE
            }
        } else {
            // Display text as a "Main Post" (text-only)
            holder.binding.tvMainContent.visibility = View.VISIBLE
            holder.binding.tvMainContent.text = post.content
            
            holder.binding.ivPostMedia.visibility = View.GONE
            holder.binding.layoutDocument.visibility = View.GONE
            holder.binding.tvContent.visibility = View.GONE
        }

        holder.binding.tvTimestamp.text = post.created_at?.substringBefore("T") ?: ""

        // Likes Logic
        val likeCount = post.likes?.size ?: 0
        holder.binding.tvLikeCount.text = likeCount.toString()
        
        val isLikedByMe = post.likes?.any { it.user_id == currentUser?.id } ?: false
        holder.binding.ivLikeIcon.setImageResource(
            if (isLikedByMe) R.drawable.ic_heart_filled_vector else R.drawable.ic_heart_vector
        )
        // Reset tint for filled icon (since it has its own color)
        if (isLikedByMe) {
            holder.binding.ivLikeIcon.imageTintList = null
        } else {
             holder.binding.ivLikeIcon.setColorFilter(context.getColor(R.color.text_primary))
        }

        holder.binding.btnLike.setOnClickListener {
            if (currentUser == null) return@setOnClickListener
            
            // Immediate UI update for "smooth" feeling
            val newLikeCount = if (isLikedByMe) likeCount - 1 else likeCount + 1
            holder.binding.tvLikeCount.text = newLikeCount.toString()
            holder.binding.ivLikeIcon.setImageResource(
                if (isLikedByMe) R.drawable.ic_heart_vector else R.drawable.ic_heart_filled_vector
            )
             if (!isLikedByMe) { // New state is Liked
                holder.binding.ivLikeIcon.imageTintList = null
            } else {
                 holder.binding.ivLikeIcon.setColorFilter(context.getColor(R.color.text_primary))
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (isLikedByMe) {
                        SupabaseConfig.client.postgrest["likes"].delete {
                            filter {
                                eq("post_id", post.id!!)
                                eq("user_id", currentUser.id)
                            }
                        }
                    } else {
                        val like = Like(post_id = post.id!!, user_id = currentUser.id)
                        SupabaseConfig.client.postgrest["likes"].insert(like)
                    }
                    withContext(Dispatchers.Main) { onAction(post.id!!) }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Sync Error", Toast.LENGTH_SHORT).show()
                        onAction(post.id!!) // Revert UI
                    }
                }
            }
        }

        // Comments - Open dedicated Activity
        val commentCount = post.comments?.size ?: 0
        holder.binding.tvCommentCount.text = "$commentCount Comments"

        holder.binding.btnComment.setOnClickListener {
            val intent = Intent(context, CommentsActivity::class.java)
            intent.putExtra("POST_ID", post.id)
            context.startActivity(intent)
        }
    }

    private fun showDeleteConfirmation(context: android.content.Context, postId: Long) {
        AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        SupabaseConfig.client.postgrest["posts"].delete {
                            filter { eq("id", postId) }
                        }
                        withContext(Dispatchers.Main) { onAction(postId) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemCount() = posts.size
}

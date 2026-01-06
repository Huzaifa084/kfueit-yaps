package com.example.kfueityaps.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.kfueityaps.databinding.ItemCommentBinding
import com.example.kfueityaps.data.model.Comment
import com.example.kfueityaps.R

class CommentsAdapter(private val comments: List<Comment>) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        holder.binding.tvCommentAuthor.text = comment.profiles?.full_name ?: "Anonymous"
        holder.binding.tvCommentContent.text = comment.content
        
        holder.binding.ivCommentAvatar.load(comment.profiles?.avatar_url) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_report_image)
            transformations(CircleCropTransformation())
        }
    }

    override fun getItemCount() = comments.size
}

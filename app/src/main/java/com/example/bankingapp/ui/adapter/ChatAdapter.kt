package com.example.bankingapp.ui.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bankingapp.databinding.ItemChatMessageBinding
import com.example.bankingapp.domain.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ViewHolder>(DiffCallback()) {
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), currentUid)
    }

    class ViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage, currentUid: String?) {
            binding.messageText.text = item.message
            val params = binding.messageContainer.layoutParams as LinearLayout.LayoutParams
            if (item.senderId == currentUid) {
                params.gravity = Gravity.END
                binding.messageContainer.setBackgroundResource(com.example.bankingapp.R.drawable.button_blue_rounded)
            } else {
                params.gravity = Gravity.START
                binding.messageContainer.setBackgroundResource(com.example.bankingapp.R.drawable.button_white_rounded)
            }
            binding.messageContainer.layoutParams = params
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.timestamp == newItem.timestamp
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
    }
}

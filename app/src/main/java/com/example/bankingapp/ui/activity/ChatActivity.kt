package com.example.bankingapp.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bankingapp.databinding.ActivityChatBinding
import com.example.bankingapp.domain.model.ChatMessage
import com.example.bankingapp.ui.adapter.ChatAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val chatAdapter = ChatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupChat()
        listenForMessages()
        
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun setupChat() {
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = chatAdapter
        }
        
        binding.sendChatBtn.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    private fun sendMessage(text: String) {
        val uid = auth.currentUser?.uid ?: return
        val messageId = database.child("support_chats").child(uid).push().key ?: return
        
        val chatMessage = ChatMessage(
            senderId = uid,
            message = text,
            timestamp = System.currentTimeMillis()
        )

        database.child("support_chats").child(uid).child(messageId).setValue(chatMessage)
            .addOnSuccessListener {
                binding.messageInput.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForMessages() {
        val uid = auth.currentUser?.uid ?: return
        database.child("support_chats").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<ChatMessage>()
                    for (postSnapshot in snapshot.children) {
                        try {
                            val message = ChatMessage().apply {
                                senderId = postSnapshot.child("senderId").value?.toString() ?: ""
                                message = postSnapshot.child("message").value?.toString() ?: ""
                                timestamp = when (val t = postSnapshot.child("timestamp").value) {
                                    is Long -> t
                                    is String -> t.toLongOrNull() ?: 0L
                                    else -> 0L
                                }
                            }
                            messages.add(message)
                        } catch (_: Exception) {
                            // Skip malformed messages
                        }
                    }
                    chatAdapter.submitList(messages)
                    if (messages.isNotEmpty()) {
                        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Chat error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

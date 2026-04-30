package com.example.bankingapp.ui.activity

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.example.bankingapp.R
import com.example.bankingapp.databinding.ActivityTransactionDetailBinding

class TransactionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val transactionName = intent.getStringExtra("transaction_name") ?: ""
        val transactionAmount = intent.getStringExtra("transaction_amount") ?: ""
        val transactionDate = intent.getStringExtra("transaction_date") ?: ""
        val transactionType = intent.getStringExtra("transaction_type") ?: ""
        val transactionCategory = intent.getStringExtra("transaction_category") ?: "Transfer"
        val transactionId = intent.getStringExtra("transaction_id") ?: ""
        val transactionImageUrl = intent.getStringExtra("transaction_image") ?: ""

        binding.transactionName.text = transactionName
        binding.transactionAmount.text = transactionAmount
        binding.dateVal.text = transactionDate
        binding.categoryVal.text = transactionCategory
        binding.idVal.text = transactionId
        
        if ((transactionType == "Sent") || transactionAmount.startsWith("-")) {
            binding.transactionAmount.setTextColor(Color.RED)
        } else {
            binding.transactionAmount.setTextColor("#4CAF50".toColorInt())
        }

        Glide.with(this)
            .load(transactionImageUrl)
            .placeholder(R.drawable.friend_1)
            .into(binding.transactionIcon)

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.shareBtn.setOnClickListener {
            // Placeholder for share functionality
        }
    }
}

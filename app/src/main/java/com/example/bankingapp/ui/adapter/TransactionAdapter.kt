package com.example.bankingapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.example.bankingapp.R
import com.example.bankingapp.databinding.ItemTransactionBinding
import com.example.bankingapp.domain.model.Transaction

import android.graphics.Color

class TransactionAdapter(private val onItemClick: (Transaction) -> Unit) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemTransactionBinding,
        private val onItemClick: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Transaction) {
            binding.transactionName.text = item.name
            binding.transactionAmount.text = item.amountString
            binding.transactionDate.text = item.date
            
            if ((item.type == "Sent") || item.amountString.startsWith("-")) {
                binding.transactionAmount.setTextColor(Color.RED)
            } else {
                binding.transactionAmount.setTextColor("#4CAF50".toColorInt())
            }
            
            Glide.with(binding.root.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.intro)
                .into(binding.transactionIcon)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem == newItem
    }
}
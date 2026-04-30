package com.example.bankingapp.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bankingapp.databinding.ItemStockBinding
import com.example.bankingapp.domain.model.Stock

class StockAdapter : ListAdapter<Stock, StockAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemStockBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Stock) {
            binding.stockSymbol.text = item.symbol
            binding.stockName.text = item.name
            binding.stockPrice.text = "$${item.price}"
            binding.stockChange.text = "${if (item.change > 0) "+" else ""}${item.change}%"
            binding.stockChange.setTextColor(if (item.change >= 0) Color.GREEN else Color.RED)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Stock>() {
        override fun areItemsTheSame(oldItem: Stock, newItem: Stock) = oldItem.symbol == newItem.symbol
        override fun areContentsTheSame(oldItem: Stock, newItem: Stock) = oldItem == newItem
    }
}

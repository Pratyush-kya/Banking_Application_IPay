package com.example.bankingapp.ui.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bankingapp.databinding.ActivityMarketBinding
import com.example.bankingapp.domain.model.Stock
import com.example.bankingapp.ui.adapter.StockAdapter
import com.example.bankingapp.ui.viewmodel.BankingViewModel
import kotlinx.coroutines.launch

class MarketActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMarketBinding
    private val viewModel: BankingViewModel by viewModels()
    private val stockAdapter = StockAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadStocks()
        observeViewModel()
        
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userData.collect { user ->
                    user?.let {
                        binding.portfolioValue.text = "Available Balance: ${it.formattedBalance}"
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.stockRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MarketActivity)
            adapter = stockAdapter
        }
    }

    private fun loadStocks() {
        val mockStocks = listOf(
            Stock("AAPL", "Apple Inc.", 185.92, 1.2),
            Stock("GOOGL", "Alphabet Inc.", 142.50, -0.5),
            Stock("TSLA", "Tesla, Inc.", 200.10, 2.8),
            Stock("AMZN", "Amazon.com, Inc.", 175.00, 0.3),
            Stock("MSFT", "Microsoft Corp.", 410.50, 1.1),
            Stock("NVDA", "NVIDIA Corp.", 720.15, 4.5),
            Stock("META", "Meta Platforms", 480.20, -1.2)
        )
        stockAdapter.submitList(mockStocks)
        binding.portfolioValue.text = "Total Portfolio Value: $2,309.37"
    }
}

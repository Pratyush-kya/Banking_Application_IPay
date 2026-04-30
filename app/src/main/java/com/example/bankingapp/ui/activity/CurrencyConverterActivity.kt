package com.example.bankingapp.ui.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bankingapp.databinding.ActivityCurrencyConverterBinding

class CurrencyConverterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCurrencyConverterBinding
    
    // Mock exchange rates (USD to X)
    private val rates = mapOf(
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 150.0,
        "INR" to 83.0,
        "BTC" to 0.000015
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currencies = rates.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpinner.adapter = adapter

        binding.convertBtn.setOnClickListener {
            val amountStr = binding.amountInput.text.toString()
            val amount = amountStr.toDoubleOrNull()
            
            if (amount != null) {
                val selectedCurrency = binding.currencySpinner.selectedItem.toString()
                val rate = rates[selectedCurrency] ?: 1.0
                val result = amount * rate
                binding.resultText.text = String.format("Result: %.2f %s", result, selectedCurrency)
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
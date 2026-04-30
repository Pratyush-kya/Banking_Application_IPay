package com.example.bankingapp.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.bankingapp.R
import com.example.bankingapp.databinding.ActivityDashboardBinding
import com.example.bankingapp.databinding.ItemTransactionBinding
import com.example.bankingapp.domain.model.Friend
import com.example.bankingapp.domain.model.User
import com.example.bankingapp.ui.viewmodel.BankingViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.bankingapp.util.DatabaseSeeder

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: BankingViewModel by viewModels()
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Initial Security Check
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 2. Seed Data (Matches your actual Soumya, Samarpit, Jivan data)
        lifecycleScope.launch { DatabaseSeeder.seed(this@DashboardActivity) }

        setupClickListeners()
        observeData()
    }

    private fun setupClickListeners() {
        binding.sendBtn.setOnClickListener { showRecipientSelectionDialog() }
        binding.receiveBtn.setOnClickListener { showReceiveDialog() }
        
        // Logout via Notification Icon
        binding.notifIcon.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
        // Navigation
        binding.profileBtn.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.profileContainer.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.chartBtn.setOnClickListener { startActivity(Intent(this, OverviewActivity::class.java)) }
        binding.marketBtn.setOnClickListener { startActivity(Intent(this, MarketActivity::class.java)) }
        binding.convertBtn.setOnClickListener { startActivity(Intent(this, CurrencyConverterActivity::class.java)) }
    }

    private fun showReceiveDialog() {
        val acc = currentUser?.accountNo ?: "100001"
        AlertDialog.Builder(this)
            .setTitle("My Account Details")
            .setMessage("Account Name: ${currentUser?.name}\nAccount Number: $acc\nCurrency: INR")
            .setPositiveButton("Copy Number") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Account", acc))
                Toast.makeText(this, "Account number copied!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showRecipientSelectionDialog() {
        val users = viewModel.allUsers.value
        if (users.isEmpty()) {
            Toast.makeText(this, "Searching for recipients...", Toast.LENGTH_SHORT).show()
            return
        }
        val names = users.map { "${it.name} (${it.accountNo})" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Select Recipient").setItems(names) { _, i ->
            showAmountDialog(users[i])
        }.show()
    }

    private fun showAmountDialog(recipient: Friend) {
        // PREVENTION: Don't allow sending to self
        if (recipient.id == currentUser?.uid || recipient.accountNo == currentUser?.accountNo) {
            Toast.makeText(this, "You cannot send money to yourself!", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this).apply { 
            hint = "0.00"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL 
        }
        AlertDialog.Builder(this)
            .setTitle("Transfer to ${recipient.name}")
            .setMessage("Enter the amount to send:")
            .setView(input)
            .setPositiveButton("Send Now") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    if (amount > (currentUser?.balance ?: 0.0)) {
                        Toast.makeText(this, "Insufficient Balance!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.initiateTransfer(recipient, amount)
                    }
                } else {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeData() {
        lifecycleScope.launch {
            // Combine User Data and All Users to ensure UI is always in sync
            combine(viewModel.userData, viewModel.allUsers) { u, all -> u to all }.collect { (u, all) ->
                if (u == null) return@collect
                
                currentUser = u
                Log.d("DashboardActivity", "UI Updating: ${u.name}, Balance: ${u.balance}")

                // 1. Headers & Balance
                binding.userName.text = u.name
                binding.balanceVal.text = u.formattedBalance
                binding.incomeVal.text = u.income
                binding.outcomeVal.text = u.outcome

                // 2. Profile Image
                if (u.profileImage.isNotEmpty()) {
                    Glide.with(this@DashboardActivity)
                        .load(u.profileImage)
                        .circleCrop()
                        .placeholder(R.drawable.friend_1)
                        .error(R.drawable.friend_1)
                        .into(binding.profileImg)
                }

                // 3. Quick Transfer Friends (The top circles)
                val friendsUI = listOf(binding.friend1, binding.friend2, binding.friend3, binding.friend4)
                friendsUI.forEach { it.visibility = View.GONE }
                
                // Show the 3 people from your database: Soumya, Samarpit, Jivan
                all.take(4).forEachIndexed { i, f ->
                    friendsUI[i].visibility = View.VISIBLE
                    Glide.with(this@DashboardActivity).load(f.imageUrl).circleCrop().placeholder(R.drawable.friend_1).into(friendsUI[i])
                    friendsUI[i].setOnClickListener { showAmountDialog(f) }
                }

                // 4. Recent Transactions
                binding.transactionList.removeAllViews()
                val recentTxs = u.transactions.values.sortedByDescending { it.timestamp }
                if (recentTxs.isEmpty()) {
                    // Show a message or leave blank
                } else {
                    recentTxs.take(5).forEach { tx ->
                        val item = ItemTransactionBinding.inflate(layoutInflater, binding.transactionList, false)
                        item.transactionName.text = tx.name
                        item.transactionAmount.text = tx.amountString
                        item.transactionDate.text = tx.date
                        
                        // Green for Received (+), Red for Sent (-)
                        if (tx.amountString.startsWith("+")) {
                            item.transactionAmount.setTextColor(Color.parseColor("#4CAF50"))
                        } else {
                            item.transactionAmount.setTextColor(Color.RED)
                        }
                        
                        Glide.with(this@DashboardActivity).load(tx.imageUrl).placeholder(R.drawable.friend_1).into(item.transactionIcon)
                        binding.transactionList.addView(item.root)
                    }
                }
            }
        }

        // Observe Transfer Results
        lifecycleScope.launch {
            viewModel.transferStatus.collect { res ->
                res?.let {
                    if (it.isSuccess) {
                        Toast.makeText(this@DashboardActivity, "Transfer Successful!", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = it.exceptionOrNull()?.message ?: "Unknown Error"
                        AlertDialog.Builder(this@DashboardActivity)
                            .setTitle("Transfer Failed")
                            .setMessage(error)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    viewModel.clearTransferStatus()
                }
            }
        }
    }
}

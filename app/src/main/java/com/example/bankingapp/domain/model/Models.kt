package com.example.bankingapp.domain.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.Locale

@IgnoreExtraProperties
data class User(
    var uid: String = "",
    var accountNumber: String = "",
    var name: String = "",
    var email: String = "",
    var balance: Double = 0.0,
    var currency: String = "INR",
    var profileImage: String = "",
    var banner: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var income: String = "₹0.00",
    var outcome: String = "₹0.00",
    var savings: String = "₹0.00",
    var overviewDate: String = "Jun 2025",
    var statistics: Map<String, Any> = emptyMap(),
    var transactions: Map<String, Transaction> = emptyMap()
) {
    val formattedBalance: String get() = String.format(Locale.US, "₹%,.2f", balance)
    
    // Alias for legacy support
    var accountNo: String 
        get() = accountNumber
        set(value) { accountNumber = value }
}

@IgnoreExtraProperties
data class Transaction(
    var transactionId: String = "",
    var id: String = "",
    var senderUid: String = "",
    var receiverUid: String = "",
    var senderAccount: String = "",
    var receiverAccount: String = "",
    var amount: Double = 0.0,
    var transactionType: String = "Transfer",
    var status: String = "Completed",
    var createdAt: Long = System.currentTimeMillis(),
    var description: String = "",
    var name: String = "",
    var imageUrl: String = "",
    var date: String = "Today",
    var type: String = "Sent",
    var category: String = "General",
    var timestamp: Long = 0,
    var amountString: String = ""
)

@IgnoreExtraProperties
data class Friend(
    var id: String = "",
    var name: String = "",
    var imageUrl: String = "",
    var accountNo: String = ""
)

@IgnoreExtraProperties
data class ChatMessage(
    var senderId: String = "",
    var message: String = "",
    var timestamp: Long = 0
)

@IgnoreExtraProperties
data class Stock(
    var symbol: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var change: Double = 0.0
)

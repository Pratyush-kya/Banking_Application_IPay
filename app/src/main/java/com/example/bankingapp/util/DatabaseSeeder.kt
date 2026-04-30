package com.example.bankingapp.util

import android.content.Context
import android.util.Log
import com.example.bankingapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object DatabaseSeeder {
    private const val DB_URL = "https://bankingapplication-01-default-rtdb.firebaseio.com"

    private val FIXED_USERS = mapOf(
        "JCtZEn4RjeWxBbY7uAgjhn5PjZP2" to User(
            uid = "JCtZEn4RjeWxBbY7uAgjhn5PjZP2", name = "Pratyush",
            accountNumber = "100001", balance = 5000.0,
            profileImage = "https://randomuser.me/api/portraits/men/1.jpg"
        ),
        "Bv7cbWhr56U0IIoBftnyFINLxXz1" to User(
            uid = "Bv7cbWhr56U0IIoBftnyFINLxXz1", name = "Soumya",
            accountNumber = "100002", balance = 7000.0,
            profileImage = "https://randomuser.me/api/portraits/women/2.jpg"
        ),
        "wr9AgslQ5ZQJVXYnuIFuQ165E1J2" to User(
            uid = "wr9AgslQ5ZQJVXYnuIFuQ165E1J2", name = "Samarpit",
            accountNumber = "100003", balance = 4500.0,
            profileImage = "https://randomuser.me/api/portraits/men/3.jpg"
        ),
        "WMaYFsro2nbFptjUJ49rB68Gtrl1" to User(
            uid = "WMaYFsro2nbFptjUJ49rB68Gtrl1", name = "Jivan",
            accountNumber = "100004", balance = 9000.0,
            profileImage = "https://randomuser.me/api/portraits/men/4.jpg"
        )
    )

    suspend fun seed(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val database = FirebaseDatabase.getInstance(DB_URL).reference
                
                FIXED_USERS.forEach { (uid, user) ->
                    val snapshot = database.child("users").child(uid).get().await()
                    if (!snapshot.exists()) {
                        val userMap: MutableMap<String, Any> = mutableMapOf(
                            "uid" to user.uid,
                            "name" to user.name,
                            "accountNumber" to user.accountNumber,
                            "accountNo" to user.accountNumber,
                            "balance" to user.balance,
                            "currency" to user.currency,
                            "profileImage" to user.profileImage,
                            "income" to "₹${String.format("%,.2f", user.balance)}",
                            "outcome" to "₹0.00",
                            "savings" to "₹0.00",
                            "overviewDate" to "Jun 2025"
                        )
                        database.child("users").child(uid).updateChildren(userMap).await()
                    }
                }
                Log.d("Seeder", "Fixed users ensured")
            } catch (e: Exception) {
                Log.e("Seeder", "Seeding failed", e)
            }
        }
    }
}

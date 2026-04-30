package com.example.bankingapp

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class BankingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Offline Persistence for the specific DB URL
        try {
            val dbUrl = "https://bankingapplication-01-default-rtdb.firebaseio.com"
            FirebaseDatabase.getInstance(dbUrl).setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Already enabled
        }
    }
}

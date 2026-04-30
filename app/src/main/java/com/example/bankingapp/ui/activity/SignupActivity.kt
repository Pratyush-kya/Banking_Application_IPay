package com.example.bankingapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bankingapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.signupButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // Initialize user with proper structure (lowercase keys, numeric balance)
                                val userMap = mapOf(
                                    "name" to name,
                                    "email" to email,
                                    "accountNo" to "100${System.currentTimeMillis() % 100000}",
                                    "balance" to 1000.0,
                                    "currency" to "INR",
                                    "profileImage" to "https://cdn-icons-png.flaticon.com/512/3135/3135715.png",
                                    "overviewDate" to "Jun 2025",
                                    "statistics" to mapOf(
                                        "Income" to "₹1,000.00",
                                        "Outcome" to "₹0.00",
                                        "Savings" to "₹500.00",
                                        "Food" to 0,
                                        "Bills" to 0,
                                        "Transfer" to 0,
                                        "Shopping" to 0
                                    )
                                )

                                val dbUrl = "https://bankingapplication-01-default-rtdb.firebaseio.com"
                                FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId)
                                    .setValue(userMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Registration Complete", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, DashboardActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "DB Sync Failed: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        } else {
                            Toast.makeText(this, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginLink.setOnClickListener { finish() }
    }
}

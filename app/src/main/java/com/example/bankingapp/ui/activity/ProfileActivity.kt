package com.example.bankingapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.bankingapp.R
import com.example.bankingapp.databinding.ActivityProfileBinding
import com.example.bankingapp.ui.viewmodel.BankingViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val viewModel: BankingViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.userData.collect { user ->
                user?.let {
                    binding.editName.setText(it.name)
                    binding.editEmail.setText(it.email)
                    // Using User.banner as profile image per current model field names or profileImage if exists
                    val profileUrl = if (it.profileImage.isNotEmpty()) it.profileImage else it.banner
                    if (profileUrl.isNotEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(profileUrl)
                            .circleCrop()
                            .placeholder(R.drawable.friend_1)
                            .into(binding.profileImage)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.saveBtn.setOnClickListener {
            val newName = binding.editName.text.toString().trim()
            if (newName.isNotEmpty()) {
                viewModel.updateProfile(newName)
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            }
        }

        binding.logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

package com.example.bankingapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bankingapp.data.repository.BankingRepository
import com.example.bankingapp.domain.model.Friend
import com.example.bankingapp.domain.model.Transaction
import com.example.bankingapp.domain.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BankingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BankingRepository(application)

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = combine(_userData, _searchQuery) { user, query ->
        val allTrans = user?.transactions?.values?.sortedByDescending { it.timestamp } ?: emptyList()
        if (query.isEmpty()) {
            allTrans
        } else {
            allTrans.filter { it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _allUsers = MutableStateFlow<List<Friend>>(emptyList())
    val allUsers: StateFlow<List<Friend>> = _allUsers.asStateFlow()

    private val _transferStatus = MutableStateFlow<Result<Unit>?>(null)
    val transferStatus: StateFlow<Result<Unit>?> = _transferStatus.asStateFlow()

    private val _graphqlStatus = MutableStateFlow<Result<Boolean>?>(null)
    val graphqlStatus: StateFlow<Result<Boolean>?> = _graphqlStatus.asStateFlow()

    data class TransferRequest(val recipient: Friend, val amount: Double)

    fun sendMoneyGraphQL(recipient: Friend, amount: Float, senderAccount: String) {
        viewModelScope.launch {
            val senderUid = userData.value?.uid ?: return@launch
            val result = repository.sendMoneyGraphQL(
                senderUid = senderUid,
                receiverUid = recipient.id,
                amount = amount,
                senderAccount = senderAccount,
                receiverAccount = recipient.accountNo
            )
            _graphqlStatus.value = result
        }
    }

    fun initiateTransfer(recipient: Friend, amount: Double) {
        Log.d("BankingViewModel", "initiateTransfer: To ${recipient.name}, Amount: $amount")
        viewModelScope.launch {
            // Option 1: Realtime Database (Current Integration)
            val result = repository.transferMoney(recipient.id, recipient.name, amount)
            
            // Option 2: Firestore (If you want to switch to Firestore)
            // val result = repository.sendMoney(senderUid = userData.value?.uid ?: "", receiverUid = recipient.id, amount = amount)
            
            _transferStatus.value = result
            if (result.isSuccess) {
                Log.d("BankingViewModel", "initiateTransfer: Success")
            } else {
                val error = result.exceptionOrNull()
                Log.e("BankingViewModel", "initiateTransfer: Failed - ${error?.message}", error)
            }
        }
    }

    fun transferByAccountNumber(accountNo: String, amount: Double) {
        Log.d("BankingViewModel", "transferByAccountNumber: Account: $accountNo, Amount: $amount")
        viewModelScope.launch {
            val friend = repository.findUserByAccountNumber(accountNo)
            if (friend != null) {
                initiateTransfer(friend, amount)
            } else {
                Log.w("BankingViewModel", "transferByAccountNumber: Account $accountNo not found")
                _transferStatus.value = Result.failure(Exception("Account number $accountNo not found"))
            }
        }
    }

    fun clearTransferStatus() {
        _transferStatus.value = null
    }

    fun logout() {
        repository.signOut()
    }

    fun updateProfile(newName: String) {
        viewModelScope.launch {
            repository.updateProfileName(newName)
        }
    }

    init {
        Log.d("BankingViewModel", "Initializing ViewModel")
        fetchUserData()
        fetchAllUsers()
    }

    private fun fetchUserData() {
        viewModelScope.launch {
            repository.getUserData().collectLatest {
                if (it != null) {
                    _userData.value = it
                }
            }
        }
    }

    private fun fetchAllUsers() {
        viewModelScope.launch {
            repository.getAllUsers().collectLatest {
                _allUsers.value = it
            }
        }
    }
}

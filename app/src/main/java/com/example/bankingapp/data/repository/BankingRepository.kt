package com.example.bankingapp.data.repository

import android.content.Context
import android.util.Log
import com.example.bankingapp.data.local.AppDatabase
import com.example.bankingapp.data.local.UserCacheEntity
import com.example.bankingapp.domain.model.Friend
import com.example.bankingapp.domain.model.Transaction
import com.example.bankingapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.coroutines.resume
import com.example.bankingapp.data.remote.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.text.SimpleDateFormat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

class BankingRepository(private val context: Context) {
    private val dbUrl = "https://bankingapplication-01-default-rtdb.firebaseio.com"
    private val database = FirebaseDatabase.getInstance(dbUrl).reference
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String? get() = auth.currentUser?.uid
    private val localDb = AppDatabase.getDatabase(context).userDao()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * FIRESTORE TRANSACTION: sendMoney
     * Deducts from sender, adds to receiver, and saves a transaction record.
     */
    suspend fun sendMoney(senderUid: String, receiverUid: String, amount: Double): Result<Unit> = try {
        val senderRef = firestore.collection("users").document(senderUid)
        val receiverRef = firestore.collection("users").document(receiverUid)
        val transactionRef = firestore.collection("transactions").document()

        firestore.runTransaction { transaction ->
            val senderDoc = transaction.get(senderRef)
            val receiverDoc = transaction.get(receiverRef)

            if (!senderDoc.exists() || !receiverDoc.exists()) {
                throw FirebaseFirestoreException("Sender or Receiver not found in Firestore", FirebaseFirestoreException.Code.NOT_FOUND)
            }

            val senderBalance = senderDoc.getDouble("balance") ?: 0.0
            val receiverBalance = receiverDoc.getDouble("balance") ?: 0.0

            if (senderBalance < amount) {
                throw FirebaseFirestoreException("Insufficient funds: balance is $senderBalance", FirebaseFirestoreException.Code.ABORTED)
            }

            // Perform Balance Updates
            transaction.update(senderRef, "balance", senderBalance - amount)
            transaction.update(receiverRef, "balance", receiverBalance + amount)

            // Log the Transaction Document
            val txData = hashMapOf(
                "senderUid" to senderUid,
                "receiverUid" to receiverUid,
                "amount" to amount,
                "timestamp" to System.currentTimeMillis(),
                "status" to "Completed",
                "type" to "Transfer"
            )
            transaction.set(transactionRef, txData)

            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepo", "sendMoney failed: ${e.message}")
        Result.failure(e)
    }

    // GraphQL Setup
    private val graphQLApi: GraphQLApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl("https://your-graphql-endpoint.com/") // Replace with actual endpoint
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GraphQLApi::class.java)
    }

    suspend fun sendMoneyGraphQL(
        senderUid: String,
        receiverUid: String,
        amount: Float,
        senderAccount: String,
        receiverAccount: String
    ): Result<Boolean> {
        val mutation = """
            mutation SendMoney(${"$"}senderUid: String!, ${"$"}receiverUid: String!, ${"$"}amount: Float!, ${"$"}senderAccount: String!, ${"$"}receiverAccount: String!) {
              sender_update: user_update(
                where: { uid: { eq: ${"$"}senderUid } }
                data: { balance: { decrement: ${"$"}amount } }
              )
              receiver_update: user_update(
                where: { uid: { eq: ${"$"}receiverUid } }
                data: { balance: { increment: ${"$"}amount } }
              )
              transaction_insert(
                data: {
                  transactionId: "${"TXN_" + System.currentTimeMillis()}",
                  senderUid: ${"$"}senderUid,
                  receiverUid: ${"$"}receiverUid,
                  senderAccount: ${"$"}senderAccount,
                  receiverAccount: ${"$"}receiverAccount,
                  amount: ${"$"}amount,
                  transactionType: "Transfer",
                  status: "Completed",
                  createdAt: "${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())}",
                  description: "Money Transfer"
                }
              )
            }
        """.trimIndent()

        val variables = mapOf(
            "senderUid" to senderUid,
            "receiverUid" to receiverUid,
            "amount" to amount,
            "senderAccount" to senderAccount,
            "receiverAccount" to receiverAccount
        )

        return try {
            val response = graphQLApi.executeMutation(GraphQLRequest(mutation, variables))
            if (response.isSuccessful && response.body()?.errors == null) {
                // Success: Trigger a refresh
                refreshUserData()
                Result.success(true)
            } else {
                val errorMsg = response.body()?.errors?.firstOrNull()?.message ?: response.message()
                Result.failure(Exception("GraphQL Error: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Refresh function to reload user data
    suspend fun refreshUserData() {
        currentUid?.let { uid ->
            // In a real GraphQL setup, you'd fetch the user again via GraphQL.
            // Here, we just trigger a Firebase re-fetch which updates the flow.
            database.child("users").child(uid).get().await()
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getUserData(): Flow<User?> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        
        // Instant load from cache
        val cacheJob = repositoryScope.launch {
            try {
                localDb.getUser(uid)?.let { trySend(it.toDomain()) }
            } catch (e: Exception) { Log.e("Repo", "Cache load error", e) }
        }
        
        val userRef = database.child("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w("Repo", "User data not found for UID: $uid")
                    return
                }

                try {
                    // Manual pull to avoid mapping issues
                    val user = User().apply {
                        this.uid = uid
                        name = snapshot.child("name").value?.toString() ?: ""
                        email = snapshot.child("email").value?.toString() ?: ""
                        balance = (snapshot.child("balance").value as? Number)?.toDouble() ?: 0.0
                        accountNumber = snapshot.child("accountNumber").value?.toString() 
                            ?: snapshot.child("accountNo").value?.toString() ?: ""
                        profileImage = snapshot.child("profileImage").value?.toString() ?: ""
                        income = snapshot.child("income").value?.toString() ?: "₹0.00"
                        outcome = snapshot.child("outcome").value?.toString() ?: "₹0.00"
                        savings = snapshot.child("savings").value?.toString() ?: "₹0.00"
                        overviewDate = snapshot.child("overviewDate").value?.toString() ?: "Jun 2025"
                    }
                    
                    val txMap = mutableMapOf<String, Transaction>()
                    snapshot.child("transactions").children.forEach { child ->
                        child.getValue(Transaction::class.java)?.let { tx ->
                            tx.transactionId = child.key ?: ""
                            tx.id = tx.transactionId
                            txMap[tx.transactionId] = tx
                        }
                    }
                    user.transactions = txMap
                    
                    // Update Cache
                    repositoryScope.launch { localDb.insertUser(UserCacheEntity.fromDomain(user)) }
                    
                    trySend(user)
                } catch (e: Exception) {
                    Log.e("Repo", "Parse error: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Repo", "Database cancelled: ${error.message}")
            }
        }
        userRef.addValueEventListener(listener)
        awaitClose { 
            userRef.removeEventListener(listener)
            cacheJob.cancel()
        }
    }

    suspend fun transferMoney(recipientId: String, recipientName: String, amount: Double): Result<Unit> = 
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val senderId = currentUid ?: run {
            continuation.resume(Result.failure(Exception("Not logged in")))
            return@suspendCancellableCoroutine
        }

        Log.d("RTDB_TX", "Starting transfer: Sender=$senderId, Receiver=$recipientId, Amount=$amount")

        // Run transaction on the "users" node
        database.child("users").runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: MutableData): com.google.firebase.database.Transaction.Result {
                // If currentData is null, return success to retry with server data
                if (currentData.value == null) {
                    Log.d("RTDB_TX", "doTransaction: currentData is null, retrying...")
                    return com.google.firebase.database.Transaction.success(currentData)
                }

                val sender = currentData.child(senderId)
                val receiver = currentData.child(recipientId)

                // Check existence
                if (sender.value == null) {
                    Log.e("RTDB_TX", "Abort: Sender $senderId not found")
                    return com.google.firebase.database.Transaction.abort()
                }
                if (receiver.value == null) {
                    Log.e("RTDB_TX", "Abort: Receiver $recipientId not found")
                    return com.google.firebase.database.Transaction.abort()
                }

                // Balance check - handle both Double and Long (Firebase might store small ints as Long)
                val sBal = (sender.child("balance").value as? Number)?.toDouble() ?: 0.0
                if (sBal < amount) {
                    Log.e("RTDB_TX", "Abort: Insufficient balance ($sBal < $amount)")
                    return com.google.firebase.database.Transaction.abort()
                }

                val rBal = (receiver.child("balance").value as? Number)?.toDouble() ?: 0.0
                
                // Perform the transfer
                sender.child("balance").value = sBal - amount
                receiver.child("balance").value = rBal + amount

                // Update metadata for summary
                sender.child("outcome").value = "₹${String.format(Locale.US, "%,.2f", amount)}"
                receiver.child("income").value = "₹${String.format(Locale.US, "%,.2f", amount)}"

                // Create Transaction Records
                val ts = System.currentTimeMillis()
                val txId = "tx_$ts"
                val sName = sender.child("name").value?.toString() ?: "User"
                val rImg = receiver.child("profileImage").value?.toString() ?: ""
                val sImg = sender.child("profileImage").value?.toString() ?: ""

                val senderTx = mapOf(
                    "id" to txId, "amount" to amount, "name" to recipientName, 
                    "imageUrl" to rImg, "type" to "Sent", "timestamp" to ts,
                    "amountString" to "-₹${String.format(Locale.US, "%,.2f", amount)}",
                    "date" to "Today", "transactionType" to "Transfer", "status" to "Completed"
                )
                val receiverTx = mapOf(
                    "id" to txId, "amount" to amount, "name" to sName, 
                    "imageUrl" to sImg, "type" to "Received", "timestamp" to ts,
                    "amountString" to "+₹${String.format(Locale.US, "%,.2f", amount)}",
                    "date" to "Today", "transactionType" to "Transfer", "status" to "Completed"
                )

                sender.child("transactions").child(txId).value = senderTx
                receiver.child("transactions").child(txId).value = receiverTx
                
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(err: DatabaseError?, committed: Boolean, snap: DataSnapshot?) {
                if (committed) {
                    Log.d("RTDB_TX", "Transfer Successful")
                    continuation.resume(Result.success(Unit))
                } else {
                    val errorMsg = err?.message ?: "Transaction Aborted (Check logs for Sender/Receiver/Balance)"
                    Log.e("RTDB_TX", "Transfer Failed: $errorMsg, Code: ${err?.code}")
                    continuation.resume(Result.failure(Exception(errorMsg)))
                }
            }
        })
    }

    fun getAllUsers(): Flow<List<Friend>> = callbackFlow {
        val uid = currentUid
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendsMap = mutableMapOf<String, Friend>()
                snapshot.children.forEach { child ->
                    val fId = child.key ?: return@forEach
                    if (fId != uid) {
                        val name = child.child("name").value?.toString() ?: "User"
                        val accNo = child.child("accountNumber").value?.toString() 
                            ?: child.child("accountNo").value?.toString() ?: ""
                        
                        // Only add if name and account number exist to avoid empty entries
                        if (name.isNotEmpty() && accNo.isNotEmpty() && accNo != "000000") {
                            friendsMap[fId] = Friend(
                                id = fId,
                                name = name,
                                imageUrl = child.child("profileImage").value?.toString() ?: "",
                                accountNo = accNo
                            )
                        }
                    }
                }
                trySend(friendsMap.values.toList().distinctBy { it.name })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("users").addValueEventListener(listener)
        awaitClose { database.child("users").removeEventListener(listener) }
    }

    suspend fun findUserByAccountNumber(acc: String): Friend? {
        val snap = database.child("users").get().await()
        for (child in snap.children) {
            val aNo = child.child("accountNumber").value?.toString() ?: child.child("accountNo").value?.toString() ?: ""
            if (aNo == acc) {
                return Friend(
                    id = child.key ?: "",
                    name = child.child("name").value?.toString() ?: "User",
                    imageUrl = child.child("profileImage").value?.toString() ?: "",
                    accountNo = acc
                )
            }
        }
        return null
    }

    suspend fun updateProfileName(newName: String) {
        currentUid?.let { database.child("users").child(it).child("name").setValue(newName) }
    }
}

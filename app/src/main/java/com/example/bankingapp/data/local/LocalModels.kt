package com.example.bankingapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bankingapp.domain.model.User

@Entity(tableName = "user_cache")
data class UserCacheEntity(
    @PrimaryKey var uid: String,
    var name: String,
    var email: String,
    var balance: Double,
    var currency: String,
    var banner: String,
    var profileImage: String,
    var accountNo: String,
    var income: String,
    var outcome: String,
    var savings: String,
    var overviewDate: String
) {
    fun toDomain() = User(
        uid = uid,
        name = name,
        email = email,
        balance = balance,
        currency = currency,
        banner = banner,
        profileImage = profileImage,
        accountNumber = accountNo,
        income = income,
        outcome = outcome,
        savings = savings,
        overviewDate = overviewDate
    )

    companion object {
        fun fromDomain(user: User) = UserCacheEntity(
            uid = user.uid,
            name = user.name,
            email = user.email,
            balance = user.balance,
            currency = user.currency,
            banner = user.banner,
            profileImage = user.profileImage,
            accountNo = user.accountNumber,
            income = user.income,
            outcome = user.outcome,
            savings = user.savings,
            overviewDate = user.overviewDate
        )
    }
}

@Entity(tableName = "friends", primaryKeys = ["userId", "friendId"])
data class FriendEntity(
    val userId: String,
    val friendId: String
)

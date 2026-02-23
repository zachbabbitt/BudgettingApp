package com.example.budgettingtogether.auth

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_pairings")
data class UserPairing(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requesterId: String,
    val receiverId: String,
    val status: String = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACCEPTED = "ACCEPTED"
    }
}

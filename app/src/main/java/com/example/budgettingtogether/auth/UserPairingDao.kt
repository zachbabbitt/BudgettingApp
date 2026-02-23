package com.example.budgettingtogether.auth

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserPairingDao {
    @Query("SELECT * FROM user_pairings WHERE (requesterId = :userId OR receiverId = :userId) AND status = 'ACCEPTED'")
    suspend fun getAcceptedPairings(userId: String): List<UserPairing>

    @Query("SELECT * FROM user_pairings WHERE receiverId = :userId AND status = 'PENDING'")
    suspend fun getPendingRequests(userId: String): List<UserPairing>

    @Query("SELECT * FROM user_pairings WHERE requesterId = :userId AND status = 'PENDING'")
    suspend fun getPendingSentRequests(userId: String): List<UserPairing>

    @Insert
    suspend fun insert(pairing: UserPairing)

    @Update
    suspend fun update(pairing: UserPairing)

    @Delete
    suspend fun delete(pairing: UserPairing)

    @Query("DELETE FROM user_pairings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM user_pairings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserPairing?

    @Query("SELECT * FROM user_pairings WHERE ((requesterId = :userId1 AND receiverId = :userId2) OR (requesterId = :userId2 AND receiverId = :userId1)) LIMIT 1")
    suspend fun findPairingBetween(userId1: String, userId2: String): UserPairing?
}

package com.example.budgettingtogether.auth

interface AuthRepository {
    suspend fun register(email: String, username: String, firstName: String, lastName: String, password: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    suspend fun getCurrentUser(userId: String): User?
}

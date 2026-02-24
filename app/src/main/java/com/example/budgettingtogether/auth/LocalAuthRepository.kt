package com.example.budgettingtogether.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local Room-backed auth. register/login are superseded by [SupabaseAuthRepository].
 * getCurrentUser is retained to read the locally cached profile after login.
 */
class LocalAuthRepository(private val userDao: UserDao) : AuthRepository {

    override suspend fun register(
        email: String, username: String, firstName: String,
        lastName: String, password: String
    ): AuthResult = AuthResult.Error("Use SupabaseAuthRepository for registration")

    override suspend fun login(email: String, password: String): AuthResult =
        AuthResult.Error("Use SupabaseAuthRepository for login")

    override suspend fun getCurrentUser(userId: String): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserById(userId)
        }
    }
}

package com.example.budgettingtogether.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAuthRepository(private val userDao: UserDao) : AuthRepository {

    override suspend fun register(email: String, username: String, firstName: String, lastName: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                if (userDao.countByEmail(email) > 0) {
                    return@withContext AuthResult.Error("An account with this email already exists")
                }
                if (userDao.countByUsername(username) > 0) {
                    return@withContext AuthResult.Error("This username is already taken")
                }

                val passwordHash = BCrypt.withDefaults().hashToString(10, password.toCharArray())
                val user = User(
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    email = email,
                    passwordHash = passwordHash
                )
                userDao.insert(user)
                AuthResult.Success(user)
            } catch (e: Exception) {
                AuthResult.Error("Registration failed: ${e.message}")
            }
        }
    }

    override suspend fun login(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUserByEmail(email)
                    ?: return@withContext AuthResult.Error("No account found with this email")

                val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
                if (!result.verified) {
                    return@withContext AuthResult.Error("Incorrect password")
                }

                AuthResult.Success(user)
            } catch (e: Exception) {
                AuthResult.Error("Login failed: ${e.message}")
            }
        }
    }

    override suspend fun getCurrentUser(userId: String): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserById(userId)
        }
    }
}

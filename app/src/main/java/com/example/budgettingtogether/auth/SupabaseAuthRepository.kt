package com.example.budgettingtogether.auth

import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.storage.dto.ProfileDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import java.util.UUID

class SupabaseAuthRepository : AuthRepository {

    private val client get() = SupabaseClientProvider.client

    override suspend fun register(
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        password: String
    ): AuthResult {
        return try {
            // Check username uniqueness before attempting sign-up
            val existing = client.from("profiles")
                .select { filter { eq("username", username) } }
                .decodeList<ProfileDto>()
            if (existing.isNotEmpty()) {
                return AuthResult.Error("This username is already taken")
            }

            // Create Supabase Auth account
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = client.auth.currentUserOrNull()?.id
                ?: return AuthResult.Error("Registration failed — could not retrieve user ID")

            // Generate a separate data-sync UUID (userGuid), distinct from the auth UUID
            val userGuid = UUID.randomUUID().toString()

            val user = User(
                id = userId,
                firstName = firstName,
                lastName = lastName,
                username = username,
                email = email,
                userGuid = userGuid
            )

            // Persist profile to Supabase so it's visible for cross-device search
            client.from("profiles").upsert(ProfileDto.fromUser(user, System.currentTimeMillis()))

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Registration failed")
        }
    }

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = client.auth.currentUserOrNull()?.id
                ?: return AuthResult.Error("Login failed — could not retrieve user ID")

            val profile = client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeList<ProfileDto>()
                .firstOrNull()
                ?: return AuthResult.Error("Login failed — profile not found")

            AuthResult.Success(profile.toUser())
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Login failed")
        }
    }

    override suspend fun getCurrentUser(userId: String): User? {
        return try {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeList<ProfileDto>()
                .firstOrNull()
                ?.toUser()
        } catch (e: Exception) {
            null
        }
    }
}

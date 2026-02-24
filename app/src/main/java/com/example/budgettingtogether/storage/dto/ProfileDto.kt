package com.example.budgettingtogether.storage.dto

import com.example.budgettingtogether.auth.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id")         val id: String,        // Supabase auth.users.id (userId)
    @SerialName("user_guid")  val userGuid: String,  // data-sync UUID (separate from auth ID)
    @SerialName("username")   val username: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name")  val lastName: String,
    @SerialName("email")      val email: String,
    @SerialName("created_at") val createdAt: Long
) {
    fun toUser() = User(
        id = id,
        firstName = firstName,
        lastName = lastName,
        username = username,
        email = email,
        createdAt = createdAt,
        userGuid = userGuid
    )

    companion object {
        fun fromUser(user: User, createdAt: Long) = ProfileDto(
            id = user.id,
            userGuid = user.userGuid,
            username = user.username,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            createdAt = createdAt
        )
    }
}

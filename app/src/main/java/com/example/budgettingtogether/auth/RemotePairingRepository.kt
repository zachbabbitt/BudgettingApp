package com.example.budgettingtogether.auth

import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.storage.dto.ProfileDto
import com.example.budgettingtogether.storage.dto.UserPairingDto
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class RemotePairingRepository {

    private val client get() = SupabaseClientProvider.client

    // ── User search ───────────────────────────────────────────────────────────

    suspend fun searchUsers(query: String, currentUserId: String): List<User> {
        if (query.isBlank()) return emptyList()
        return try {
            client.from("profiles")
                .select {
                    filter {
                        or {
                            ilike("username", "%${query.trim()}%")
                            ilike("email", "%${query.trim()}%")
                        }
                        neq("id", currentUserId)
                    }
                }
                .decodeList<ProfileDto>()
                .map { it.toUser() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserById(userId: String): User? {
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

    // ── Pairing operations ────────────────────────────────────────────────────

    suspend fun sendPairingRequest(requesterId: String, receiverId: String): Result<Unit> {
        return try {
            // Check if a pairing already exists between these two users (either direction)
            val existing = client.from("user_pairings")
                .select {
                    filter {
                        or {
                            and {
                                eq("requester_id", requesterId)
                                eq("receiver_id", receiverId)
                            }
                            and {
                                eq("requester_id", receiverId)
                                eq("receiver_id", requesterId)
                            }
                        }
                    }
                }
                .decodeList<UserPairingDto>()

            if (existing.isNotEmpty()) {
                return Result.failure(Exception("A pairing request already exists between these users"))
            }

            client.from("user_pairings").insert(
                UserPairingDto(
                    requesterId = requesterId,
                    receiverId = receiverId,
                    createdAt = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptPairingRequest(pairingId: Long, receiverUserId: String): Result<Unit> {
        return try {
            val pairing = client.from("user_pairings")
                .select { filter { eq("id", pairingId) } }
                .decodeList<UserPairingDto>()
                .firstOrNull()
                ?: return Result.failure(Exception("Pairing request not found"))

            if (pairing.receiverId != receiverUserId) {
                return Result.failure(Exception("Only the receiver can accept this request"))
            }

            client.from("user_pairings")
                .update(PairingAcceptUpdate(UserPairing.STATUS_ACCEPTED, System.currentTimeMillis())) {
                    filter { eq("id", pairingId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPairingRequest(pairingId: Long) {
        try {
            client.from("user_pairings").delete { filter { eq("id", pairingId) } }
        } catch (_: Exception) {}
    }

    suspend fun unpair(currentUserId: String, partnerId: String) {
        try {
            client.from("user_pairings").delete {
                filter {
                    or {
                        and {
                            eq("requester_id", currentUserId)
                            eq("receiver_id", partnerId)
                        }
                        and {
                            eq("requester_id", partnerId)
                            eq("receiver_id", currentUserId)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // ── Partner queries ───────────────────────────────────────────────────────

    suspend fun getPartners(currentUserId: String): List<User> {
        return try {
            val pairings = client.from("user_pairings")
                .select {
                    filter {
                        or {
                            eq("requester_id", currentUserId)
                            eq("receiver_id", currentUserId)
                        }
                        eq("status", UserPairing.STATUS_ACCEPTED)
                    }
                }
                .decodeList<UserPairingDto>()

            val partnerIds = pairings.map { pairing ->
                if (pairing.requesterId == currentUserId) pairing.receiverId else pairing.requesterId
            }
            if (partnerIds.isEmpty()) return emptyList()

            client.from("profiles")
                .select { filter { isIn("id", partnerIds) } }
                .decodeList<ProfileDto>()
                .map { it.toUser() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPairedUserGuids(currentUserId: String, currentUserGuid: String): List<String> {
        return try {
            val pairings = client.from("user_pairings")
                .select {
                    filter {
                        or {
                            eq("requester_id", currentUserId)
                            eq("receiver_id", currentUserId)
                        }
                        eq("status", UserPairing.STATUS_ACCEPTED)
                    }
                }
                .decodeList<UserPairingDto>()

            if (pairings.isEmpty()) return listOf(currentUserGuid)

            val partnerIds = pairings.map { pairing ->
                if (pairing.requesterId == currentUserId) pairing.receiverId else pairing.requesterId
            }

            val partnerGuids = client.from("profiles")
                .select { filter { isIn("id", partnerIds) } }
                .decodeList<ProfileDto>()
                .map { it.userGuid }

            listOf(currentUserGuid) + partnerGuids
        } catch (e: Exception) {
            listOf(currentUserGuid)
        }
    }

    suspend fun getPendingReceivedRequests(currentUserId: String): List<UserPairing> {
        return try {
            client.from("user_pairings")
                .select {
                    filter {
                        eq("receiver_id", currentUserId)
                        eq("status", UserPairing.STATUS_PENDING)
                    }
                }
                .decodeList<UserPairingDto>()
                .map { it.toEntity() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPendingSentRequests(currentUserId: String): List<UserPairing> {
        return try {
            client.from("user_pairings")
                .select {
                    filter {
                        eq("requester_id", currentUserId)
                        eq("status", UserPairing.STATUS_PENDING)
                    }
                }
                .decodeList<UserPairingDto>()
                .map { it.toEntity() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @Serializable
    private data class PairingAcceptUpdate(
        val status: String,
        @SerialName("accepted_at") val acceptedAt: Long
    )
}

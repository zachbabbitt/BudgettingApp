package com.example.budgettingtogether.auth

class PairingRepository(
    private val userDao: UserDao,
    private val userPairingDao: UserPairingDao
) {
    suspend fun searchUsers(query: String, currentUserId: String): List<User> {
        if (query.isBlank()) return emptyList()
        return userDao.searchUsers(query.trim(), currentUserId)
    }

    suspend fun sendPairingRequest(requesterId: String, receiverId: String): Result<Unit> {
        val existing = userPairingDao.findPairingBetween(requesterId, receiverId)
        if (existing != null) {
            return Result.failure(Exception("A pairing request already exists between these users"))
        }
        userPairingDao.insert(
            UserPairing(
                requesterId = requesterId,
                receiverId = receiverId,
                status = UserPairing.STATUS_PENDING
            )
        )
        return Result.success(Unit)
    }

    suspend fun acceptPairingRequest(pairingId: Long, receiverUserId: String): Result<Unit> {
        val pairing = userPairingDao.getById(pairingId)
            ?: return Result.failure(Exception("Pairing request not found"))
        if (pairing.receiverId != receiverUserId) {
            return Result.failure(Exception("Only the receiver can accept this request"))
        }
        userPairingDao.update(
            pairing.copy(
                status = UserPairing.STATUS_ACCEPTED,
                acceptedAt = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun rejectPairingRequest(pairingId: Long) {
        userPairingDao.deleteById(pairingId)
    }

    suspend fun unpair(currentUserId: String, partnerId: String) {
        val pairing = userPairingDao.findPairingBetween(currentUserId, partnerId)
        if (pairing != null) {
            userPairingDao.delete(pairing)
        }
    }

    suspend fun getPartners(currentUserId: String): List<User> {
        val pairings = userPairingDao.getAcceptedPairings(currentUserId)
        val partnerIds = pairings.map { pairing ->
            if (pairing.requesterId == currentUserId) pairing.receiverId else pairing.requesterId
        }
        if (partnerIds.isEmpty()) return emptyList()
        return userDao.getUsersByIds(partnerIds)
    }

    suspend fun getPairedUserGuids(currentUserId: String, currentUserGuid: String): List<String> {
        val pairings = userPairingDao.getAcceptedPairings(currentUserId)
        if (pairings.isEmpty()) return listOf(currentUserGuid)

        val partnerIds = pairings.map { pairing ->
            if (pairing.requesterId == currentUserId) pairing.receiverId else pairing.requesterId
        }
        val partners = userDao.getUsersByIds(partnerIds)
        return listOf(currentUserGuid) + partners.map { it.userGuid }
    }

    suspend fun getPendingReceivedRequests(currentUserId: String): List<UserPairing> {
        return userPairingDao.getPendingRequests(currentUserId)
    }

    suspend fun getPendingSentRequests(currentUserId: String): List<UserPairing> {
        return userPairingDao.getPendingSentRequests(currentUserId)
    }
}

package com.hhplus.concert.business.domain.manager.queue

import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.util.JwtUtil
import com.hhplus.concert.infrastructure.redis.QueueRedisRepository
import org.springframework.stereotype.Component

@Component
class QueueManager(
    private val queueRedisRepository: QueueRedisRepository,
    private val jwtUtil: JwtUtil,
) {
    // JWT Token 을 userId 로 생성하고, QUEUE 를 생성한다.
    fun enqueueAndIssueToken(userId: Long): String {
        val token = jwtUtil.generateToken(userId)
        val score = System.currentTimeMillis()
        val existingToken = queueRedisRepository.findWaitingQueueTokenByUserId(userId.toString())

        existingToken?.let {
            queueRedisRepository.removeFromWaitingQueue(it, userId.toString())
        }

        queueRedisRepository.addToWaitingQueue(token, userId.toString(), score)
        return token
    }

    fun getQueueStatus(token: String): QueueStatus {
        val userId = jwtUtil.getUserIdFromToken(token)

        return when {
            queueRedisRepository.isProcessingQueue(token) -> QueueStatus.PROCESSING
            queueRedisRepository.getWaitingQueuePosition(token, userId.toString()) > 0L -> QueueStatus.WAITING
            else -> QueueStatus.CANCELLED
        }
    }

    fun getPositionInWaitingStatus(token: String): Long {
        val userId = jwtUtil.getUserIdFromToken(token)
        return queueRedisRepository.getWaitingQueuePosition(token, userId.toString())
    }

    fun updateToProcessingTokens() {
        val availableProcessingRoom = calculateAvailableProcessingRoom()
        if (availableProcessingRoom <= 0) return

        val tokensNeedToUpdateToProcessing =
            queueRedisRepository.getWaitingQueueNeedToUpdateToProcessing(availableProcessingRoom.toInt())

        tokensNeedToUpdateToProcessing.forEach { (token, userId) ->
            queueRedisRepository.updateToProcessingQueue(
                token = token,
                userId = userId,
                expirationTime = System.currentTimeMillis() + TOKEN_EXPIRATION_TIME,
            )
        }
    }

    private fun calculateAvailableProcessingRoom(): Long {
        val currentProcessingCount = queueRedisRepository.getProcessingQueueCount()
        return (ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT - currentProcessingCount).coerceAtLeast(0)
    }

    fun removeExpiredWaitingQueue() {
        queueRedisRepository.removeExpiredWaitingQueue(System.currentTimeMillis())
    }

    fun completeProcessingToken(token: String) {
        queueRedisRepository.removeProcessingToken(token)
    }

    /**
     * BatchSize : 서버가 한 번에 1000명의 대기자를 처리할 수 있다고 가정한다.
     * BatchInterval : 1000명의 대기자를 처리하는 데 약 5분이 걸린다고 가정한다. (대략 콘서트 예약을 완료하는 데 5분 이 걸린다고 가정)
     * Batches : 주어진 position (조회한 WAITING 상태의 대기열의 현재 위치) 이 처리되기까지 필요한 batch 수
     * 결과 값 : 필요한 batch 수에 각 배치 처리시간을 곱하여 예상되는 대기 시간을 계산한다.
     */
    fun calculateEstimatedWaitSeconds(position: Long): Long {
        val batchSize = 1000L
        val batchInterval = 60L * 5 // 5 minutes
        val batches = position / batchSize
        return batches * batchInterval
    }

    companion object {
        const val ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT = 1000
        const val TOKEN_EXPIRATION_TIME = 15L * 60L * 1000 // 15 minutes
    }
}

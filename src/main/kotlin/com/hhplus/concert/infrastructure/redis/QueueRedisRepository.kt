package com.hhplus.concert.infrastructure.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class QueueRedisRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    /**
     * 대기열을 등록한다.
     */
    fun addToWaitingQueue(
        token: String,
        userId: String,
        expirationTime: Long,
    ) {
        redisTemplate.opsForZSet().add(WAITING_QUEUE_KEY, "$token:$userId", expirationTime.toDouble())
    }

    fun findWaitingQueueTokenByUserId(userId: String): String? {
        val pattern = "*:$userId"
        return redisTemplate.opsForZSet().rangeByScore(WAITING_QUEUE_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
            ?.find { it.endsWith(pattern) }
            ?.split(":")
            ?.firstOrNull()
    }

    fun removeFromWaitingQueue(
        token: String,
        userId: String,
    ) {
        redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, "$token:$userId")
    }

    /**
     * WAITING 상태의 현재 대기열을 삭제하고, PROCESSING 상태를 등록한다.
     */
    fun updateToProcessingQueue(
        token: String,
        userId: String,
        expirationTime: Long,
    ) {
        redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, "$token:$userId")
        redisTemplate.opsForZSet().add(PROCESSING_QUEUE_KEY, "$token:$userId", expirationTime.toDouble())
    }

    /**
     * 조회한 Token 의 대기열이 PROCESSING 상태인지 확인한다.
     */
    fun isProcessingQueue(token: String): Boolean {
        val score = redisTemplate.opsForZSet().score(PROCESSING_QUEUE_KEY, "$token:*")
        return score != null
    }

    /**
     * 현재 WAITING 상태의 대기열이 몇번째인지 순서를 리턴한다.
     */
    fun getWaitingQueuePosition(
        token: String,
        userId: String,
    ): Long = redisTemplate.opsForZSet().rank(WAITING_QUEUE_KEY, "$token:$userId") ?: -1

    /**
     * 현재 WAITING 상태의 대기열이 총 몇개인지 확인한다.
     */
    fun getWaitingQueueSize(): Long = redisTemplate.opsForZSet().size(WAITING_QUEUE_KEY) ?: 0

    /**
     * 현재 PROCESSING 상태의 대기열이 총 몇개인지 확인한다.
     */
    fun getProcessingQueueCount(): Long = redisTemplate.opsForZSet().size(PROCESSING_QUEUE_KEY) ?: 0

    /**
     * WAITING 상태의 대기열 중 PROCESSING 상태로 변경 할 수 있는 수만큼의 WAITING 상태의 대기열을 가지고 온다.
     */
    fun getWaitingQueueNeedToUpdateToProcessing(needToUpdateCount: Int): List<Pair<String, String>> =
        redisTemplate.opsForZSet().range(WAITING_QUEUE_KEY, 0, needToUpdateCount.toLong() - 1)
            ?.map {
                val (token, userId) = it.split(":")
                token to userId
            } ?: emptyList()

    /**
     * 현재 WAITING 상태의 대기열 중, 만료된 (ExpirationTime 이 현재시간보다 이전인) 대기열을 삭제한다.
     */
    fun removeExpiredWaitingQueue(currentTime: Long) {
        redisTemplate.opsForZSet().removeRangeByScore(WAITING_QUEUE_KEY, Double.NEGATIVE_INFINITY, currentTime.toDouble())
    }

    /**
     * 취소되거나 완료된 상태의 PROCESSING 대기열을 삭제한다.
     */
    fun removeProcessingToken(token: String) {
        val pattern = "$token:*"
        redisTemplate.opsForSet().members(PROCESSING_QUEUE_KEY)?.find { it.startsWith(pattern) }?.let {
            redisTemplate.opsForSet().remove(PROCESSING_QUEUE_KEY, it)
        }
    }

    companion object {
        const val WAITING_QUEUE_KEY = "waiting_queue"
        const val PROCESSING_QUEUE_KEY = "processing_queue"
    }
}

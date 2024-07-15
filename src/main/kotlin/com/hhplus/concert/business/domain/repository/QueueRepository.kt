package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.infrastructure.entity.Queue

interface QueueRepository {
    fun findByUserIdAndStatus(
        userId: Long,
        queueStatus: QueueStatus,
    ): Queue?

    fun save(queue: Queue)

    fun findByToken(token: String): Queue?

    fun getPositionInStatus(
        queueId: Long,
        queueStatus: QueueStatus,
    ): Int

    fun countByQueueStatus(queueStatus: QueueStatus): Int

    fun findTopByStatusOrderByIdAsc(
        queueStatus: QueueStatus,
        limit: Int,
    ): List<Queue>

    fun updateStatusForIds(
        queueIds: List<Long>,
        queueStatus: QueueStatus,
    )

    fun deleteAll()

    fun findAll(): List<Queue>
}

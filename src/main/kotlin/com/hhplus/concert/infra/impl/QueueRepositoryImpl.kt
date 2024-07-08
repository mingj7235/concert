package com.hhplus.concert.infra.impl

import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.domain.repository.QueueRepository
import com.hhplus.concert.infra.entity.Queue
import com.hhplus.concert.infra.jpa.QueueJpaRepository
import org.springframework.stereotype.Repository

@Repository
class QueueRepositoryImpl(
    private val queueJpaRepository: QueueJpaRepository,
) : QueueRepository {
    override fun findByUserIdAndStatus(
        userId: Long,
        queueStatus: QueueStatus,
    ): Queue? =
        queueJpaRepository.findByUserIdAndStatus(
            userId = userId,
            queueStatus = queueStatus,
        )

    override fun save(queue: Queue) {
        queueJpaRepository.save(queue)
    }

    override fun findByToken(token: String): Queue? = queueJpaRepository.findByToken(token)

    override fun getPositionInStatus(
        queueId: Long,
        queueStatus: QueueStatus,
    ): Int = queueJpaRepository.getPositionInStatus(queueId, queueStatus)

    override fun countByQueueStatus(queueStatus: QueueStatus): Int = queueJpaRepository.countByQueueStatus(queueStatus)

    override fun findTopByStatusOrderByIdAsc(
        queueStatus: QueueStatus,
        limit: Int,
    ): List<Queue> = queueJpaRepository.findTopByStatusOrderByIdAsc(queueStatus, limit)

    override fun updateStatusForIds(
        queueIds: List<Long>,
        queueStatus: QueueStatus,
    ) {
        queueJpaRepository.updateStatusForIds(queueIds, queueStatus)
    }

    override fun deleteAll() {
        queueJpaRepository.deleteAll()
    }

    override fun findAll(): List<Queue> = queueJpaRepository.findAll()
}

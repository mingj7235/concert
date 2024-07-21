package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.entity.Queue
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.infrastructure.jpa.QueueJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Repository
class QueueRepositoryImpl(
    private val queueJpaRepository: QueueJpaRepository,
) : QueueRepository {
    override fun findById(queueId: Long): Queue? = queueJpaRepository.findById(queueId).getOrNull()

    override fun findByUserIdAndStatus(
        userId: Long,
        queueStatus: QueueStatus,
    ): Queue? =
        queueJpaRepository.findByUserIdAndStatus(
            userId = userId,
            queueStatus = queueStatus,
        )

    override fun save(queue: Queue): Queue? = queueJpaRepository.save(queue)

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

    override fun findExpiredWaitingQueueIds(
        queueStatus: QueueStatus,
        expiredAt: LocalDateTime,
    ): List<Long> = queueJpaRepository.findExpiredWaitingQueueIds(queueStatus, expiredAt)
}

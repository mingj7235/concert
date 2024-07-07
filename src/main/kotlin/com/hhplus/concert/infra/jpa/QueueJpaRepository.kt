package com.hhplus.concert.infra.jpa

import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.infra.entity.Queue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface QueueJpaRepository : JpaRepository<Queue, Long> {
    @Query("select queue from Queue queue where queue.user = :userId and queue.queueStatus = :queueStatus")
    fun findByUserIdAndStatus(
        userId: Long,
        queueStatus: QueueStatus,
    ): Queue?

    fun findByToken(token: String): Queue?

    @Query("select count(queue) from Queue queue where queue.queueStatus = :queueStatus and queue.id < :queueId")
    fun getPositionInStatus(
        queueId: Long,
        queueStatus: QueueStatus,
    ): Int

    fun countByQueueStatus(queueStatus: QueueStatus): Int

    @Query("select queue from Queue queue where queue.queueStatus = :queueStatus order by queue.id asc limit :limit")
    fun findTopByStatusOrderByIdAsc(
        queueStatus: QueueStatus,
        limit: Int,
    ): List<Queue>

    @Modifying
    @Query("update Queue queue set queue.queueStatus = :queueStatus where queue.id in :queueIds")
    fun updateStatusForIds(
        queueIds: List<Long>,
        queueStatus: QueueStatus,
    )
}

package com.hhplus.concert.domain.manager.queue

import com.hhplus.concert.common.exception.error.QueueException
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.util.JwtUtil
import com.hhplus.concert.domain.repository.QueueRepository
import com.hhplus.concert.infra.entity.Queue
import com.hhplus.concert.infra.entity.User
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class QueueManager(
    private val queueRepository: QueueRepository,
    private val jwtUtil: JwtUtil,
) {
    // userId 와 queueStatus 로 queue 를 찾는다.
    fun findByUserIdAndStatus(
        userId: Long,
        queueStatus: QueueStatus,
    ): Queue? =
        queueRepository.findByUserIdAndStatus(
            userId = userId,
            queueStatus = queueStatus,
        )

    // queue 를 생성하고, token 을 생성한다.
    fun enqueueAndIssueToken(user: User): String {
        val token = jwtUtil.generateToken(user.id)

        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.WAITING,
            ),
        )

        return token
    }

    // queue 의 상태를 변경한다.
    fun updateStatus(
        queue: Queue,
        queueStatus: QueueStatus,
    ) {
        queue.updateStatus(queueStatus)
        queueRepository.save(queue)
    }

    fun updateStatus(
        queueIds: List<Long>,
        queueStatus: QueueStatus,
    ) {
        if (queueIds.isEmpty()) throw QueueException.InvalidRequest()
        queueRepository.updateStatusForIds(queueIds, queueStatus)
    }

    fun findByToken(token: String) = queueRepository.findByToken(token) ?: throw QueueException.QueueNotFound()

    /**
     * 현재 waiting list 중에서 조회하려는 queue 가 몇번째에 있는지를 확인한다.
     * service 로직에서 현재 조회하려는 queue 는 waiting 상태임을 확인한다.
     */
    fun getPositionInWaitingStatus(queueId: Long): Int = queueRepository.getPositionInStatus(queueId, QueueStatus.WAITING)

    fun countByQueueStatus(queueStatus: QueueStatus): Int = queueRepository.countByQueueStatus(queueStatus)

    /**
     * queue 의 상태가 waiting 인 queue 중에서 순서대로 processing 상태로 변경될 queue 들의 id list 를 찾는다.
     */
    fun getNeededUpdateToProcessingIdsFromWaiting(neededToUpdateCount: Int): List<Long> =
        queueRepository
            .findTopByStatusOrderByIdAsc(
                queueStatus = QueueStatus.WAITING,
                limit = neededToUpdateCount,
            ).map { it.id }
}
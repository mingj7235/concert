package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.application.dto.QueueServiceDto
import com.hhplus.concert.business.domain.manager.UserManager
import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.common.type.QueueStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueueService(
    private val queueManager: QueueManager,
    private val userManager: UserManager,
) {
    /**
     * userId 를 통해 user 를 찾아온다.
     * waiting 상태로 queue 저장 및 token 발급
     */
    @Transactional
    fun issueQueueToken(userId: Long): QueueServiceDto.IssuedToken {
        val user = userManager.findById(userId)

        return QueueServiceDto.IssuedToken(
            token = queueManager.enqueueAndIssueToken(user.id),
        )
    }

    /**
     * token 을 통해 queue 의 상태를 조회한다.
     * 현재 queue 상태가 waiting 상태라면 현재 대기열이 얼마나 남았는지를 계산하여 반환한다.
     * 그 밖의 상태라면, 얼마나 대기를 해야하는지 알 필요가 없으므로 0 을 반환한다.
     */
    fun findQueueByToken(token: String): QueueServiceDto.Queue {
        val status = queueManager.getQueueStatus(token)
        val isWaiting = status == QueueStatus.WAITING

        val position = if (isWaiting) queueManager.getPositionInWaitingStatus(token) else NO_REMAINING_WAIT
        val estimatedWaitTime = if (isWaiting) queueManager.calculateEstimatedWaitSeconds(position) else NO_REMAINING_WAIT

        return QueueServiceDto.Queue(
            status = status,
            remainingWaitListCount = position,
            estimatedWaitTime = estimatedWaitTime,
        )
    }

    /**
     * 스케쥴러를 통해 WAITING 상태의 대기열을 PROCESSING 상태로 변경한다.
     */
    fun updateToProcessingTokens() {
        queueManager.updateToProcessingTokens()
    }

    /**
     * 스케쥴러를 통해 만료 시간이 지났지만 여전히 WAITING 상태인 대기열을 삭제한다.
     */
    fun cancelExpiredWaitingQueue() {
        queueManager.removeExpiredWaitingQueue()
    }

    companion object {
        const val NO_REMAINING_WAIT = 0L
    }
}

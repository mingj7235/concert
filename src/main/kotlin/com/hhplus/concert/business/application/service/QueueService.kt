package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.application.dto.QueueServiceDto
import com.hhplus.concert.business.domain.manager.QueueManager
import com.hhplus.concert.business.domain.manager.UserManager
import com.hhplus.concert.common.type.QueueStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class QueueService(
    private val queueManager: QueueManager,
    private val userManager: UserManager,
) {
    /**
     * userId 전제 검증
     * 현재 웨이팅 상태인 queue 가 저장되어있는지 검증
     * 있다면 기존 queue 상태를 초기화, 대기열에서 밀려남
     * waiting 상태로 queue 저장 및 token 발급
     */
    @Transactional
    fun issueQueueToken(userId: Long): QueueServiceDto.IssuedToken {
        val user = userManager.findById(userId)
        val queue =
            queueManager.findByUserIdAndStatus(
                userId = user.id,
                queueStatus = QueueStatus.WAITING,
            )

        // queue 가 기존에 존재한다면, 그 queue 를 취소상태로 변경한다.
        if (queue != null) {
            queueManager.updateStatus(queue, QueueStatus.CANCELLED)
        }

        return QueueServiceDto.IssuedToken(
            token = queueManager.enqueueAndIssueToken(user),
            createdAt = LocalDateTime.now(),
        )
    }

    /**
     * token 을 통해 queue 의 정보를 반환한다.
     * 현재 queue 상태가 waiting 상태라면 현재 대기열이 얼마나 남았는지를 계산하여 반환한다.
     * 그 밖의 상태라면, 얼마나 대기를 해야하는지 알 필요가 없으므로 0 을 반환한다.
     */
    @Transactional
    fun findQueueByToken(token: String): QueueServiceDto.Queue {
        val queue = queueManager.findByToken(token)

        return QueueServiceDto.Queue(
            queueId = queue.id,
            status = queue.queueStatus,
            joinAt = queue.joinedAt,
            remainingWaitListCount =
                if (queue.queueStatus == QueueStatus.WAITING) {
                    queueManager.getPositionInWaitingStatus(queue.id)
                } else {
                    NO_REMAINING_WAIT_LIST_COUNT
                },
        )
    }

    /**
     * 스케쥴러를 통해 queue 의 process 상태인 상태의 queue 개수를 유지시킨다.
     */
    @Transactional
    fun maintainProcessingCount() {
        val neededToUpdateCount = ALLOWED_MAX_SIZE - queueManager.countByQueueStatus(QueueStatus.PROCESSING)

        if (neededToUpdateCount > 0) {
            queueManager.updateStatus(
                queueIds = queueManager.getNeededUpdateToProcessingIdsFromWaiting(neededToUpdateCount),
                queueStatus = QueueStatus.PROCESSING,
            )
        }
    }

    /**
     * 스케쥴러를 통해 일정 시간이 지났지만 여전히 WAITING 상태인 queue 를 CANCELLED 로 변환시킨다.
     */
    @Transactional
    fun cancelExpiredWaitingQueue() {
        queueManager.updateStatus(
            queueIds = queueManager.getExpiredWaitingQueueIds(),
            queueStatus = QueueStatus.CANCELLED,
        )
    }

    companion object {
        const val NO_REMAINING_WAIT_LIST_COUNT = 0
        const val ALLOWED_MAX_SIZE = 100
    }
}

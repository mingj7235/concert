package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.domain.entity.Queue
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.type.QueueStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QueueSchedulerIntegrationTest {
    @Autowired
    private lateinit var queueScheduler: QueueScheduler

    @Autowired
    private lateinit var queueRepository: QueueRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Nested
    @DisplayName("[maintainProcessingCount] 테스트")
    inner class MaintainProcessingCountTest {
        @Test
        fun `스케줄러가 PROCESSING 상태의 100개의 큐 개수를 유지해야 한다`() {
            // given
            val processingCount = 95
            val waitingCount = 10
            createQueues(processingCount, QueueStatus.PROCESSING)
            createQueues(waitingCount, QueueStatus.WAITING)

            // when
            queueScheduler.maintainProcessingCount()

            val updatedProcessingCount = queueRepository.countByQueueStatus(QueueStatus.PROCESSING)
            val updatedWaitingCount = queueRepository.countByQueueStatus(QueueStatus.WAITING)

            assertEquals(ALLOWED_QUEUE_MAX_SIZE, updatedProcessingCount)
            assertEquals(waitingCount - (ALLOWED_QUEUE_MAX_SIZE - processingCount), updatedWaitingCount)
        }

        @Test
        fun `PROCESSING 상태의 큐가 이미 최대 개수일 때 스케줄러가 아무 작업도 수행하지 않아야 한다`() {
            // given
            createQueues(ALLOWED_QUEUE_MAX_SIZE, QueueStatus.PROCESSING)
            createQueues(5, QueueStatus.WAITING)

            // when
            queueScheduler.maintainProcessingCount()

            val updatedProcessingCount = queueRepository.countByQueueStatus(QueueStatus.PROCESSING)
            val updatedWaitingCount = queueRepository.countByQueueStatus(QueueStatus.WAITING)

            assertEquals(ALLOWED_QUEUE_MAX_SIZE, updatedProcessingCount)
            assertEquals(5, updatedWaitingCount)
        }

        @Test
        fun `WAITING 상태의 큐가 없을 때 스케줄러가 아무 작업도 수행하지 않아야 한다`() {
            // given
            val processingCount = 3
            createQueues(processingCount, QueueStatus.PROCESSING)

            // when
            queueScheduler.maintainProcessingCount()

            val updatedProcessingCount = queueRepository.countByQueueStatus(QueueStatus.PROCESSING)
            val updatedWaitingCount = queueRepository.countByQueueStatus(QueueStatus.WAITING)

            assertEquals(processingCount, updatedProcessingCount)
            assertEquals(0, updatedWaitingCount)
        }

        private fun createQueues(
            count: Int,
            status: QueueStatus,
        ) {
            repeat(count) {
                val user = userRepository.save(User(name = "User $it"))
                queueRepository.save(
                    Queue(
                        user = user,
                        token = "token_$it",
                        joinedAt = LocalDateTime.now(),
                        queueStatus = status,
                    ),
                )
            }
        }
    }

    @Nested
    @DisplayName("[cancelExpiredWaitingQueue] 테스트")
    inner class CancelExpiredWaitingQueueTest {
        @Test
        fun `만료된 대기 큐는 취소됨`() {
            // Given
            val now = LocalDateTime.now()

            val user1 = userRepository.save(User(name = "User1"))
            val user2 = userRepository.save(User(name = "User2"))

            val expiredQueue =
                Queue(
                    user = user1,
                    token = "token1",
                    joinedAt = now.minusHours(2),
                    queueStatus = QueueStatus.WAITING,
                )

            val activeQueue =
                Queue(
                    user = user2,
                    token = "token2",
                    joinedAt = now.minusMinutes(30),
                    queueStatus = QueueStatus.WAITING,
                )

            queueRepository.save(expiredQueue)
            queueRepository.save(activeQueue)

            // When
            queueScheduler.cancelExpiredWaitingQueue()

            // Then
            val updatedExpiredQueue = queueRepository.findById(expiredQueue.id)
            val updatedActiveQueue = queueRepository.findById(activeQueue.id)

            assertEquals(QueueStatus.CANCELLED, updatedExpiredQueue!!.queueStatus)
            assertEquals(QueueStatus.WAITING, updatedActiveQueue!!.queueStatus)
        }

        @Test
        fun `취소된 큐는 다시 취소되지 않음`() {
            // Given
            val now = LocalDateTime.now()
            val user1 = userRepository.save(User(name = "User1"))
            val cancelledQueue =
                Queue(
                    user = user1,
                    token = "token1",
                    joinedAt = now.minusHours(2),
                    queueStatus = QueueStatus.CANCELLED,
                )

            queueRepository.save(cancelledQueue)

            // When
            queueScheduler.cancelExpiredWaitingQueue()

            // Then
            val updatedQueue = queueRepository.findById(cancelledQueue.id)
            assertEquals(QueueStatus.CANCELLED, updatedQueue!!.queueStatus)
        }
    }

    companion object {
        const val ALLOWED_QUEUE_MAX_SIZE = 100
    }
}

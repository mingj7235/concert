package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.infrastructure.entity.Queue
import com.hhplus.concert.infrastructure.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
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

    companion object {
        const val ALLOWED_QUEUE_MAX_SIZE = 100
    }
}

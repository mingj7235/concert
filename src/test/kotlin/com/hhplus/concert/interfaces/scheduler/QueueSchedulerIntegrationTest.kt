package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.infrastructure.redis.QueueRedisRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
@AutoConfigureMockMvc
class QueueSchedulerIntegrationTest {
    @Autowired
    private lateinit var queueScheduler: QueueScheduler

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var queueManager: QueueManager

    @Autowired
    private lateinit var queueRedisRepository: QueueRedisRepository

    @BeforeEach
    fun setup() {
        redisTemplate.execute { connection ->
            connection.flushAll()
        }
    }

    @Nested
    @DisplayName("[activateWaitingTokens] 테스트")
    inner class ActivateWaitingTokensTest {
        @Test
        fun `스케줄러가 WAITING 상태의 토큰을 PROCESSING 상태로 전환해야 한다`() {
            // given
            val waitingCount = 1050
            createTokens(waitingCount)

            // when
            queueScheduler.updateToProcessingTokens()

            // then
            val updatedProcessingCount = queueRedisRepository.getProcessingQueueCount()
            val updatedWaitingCount = queueRedisRepository.getWaitingQueueSize()

            assertEquals(QueueManager.ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT.toLong(), updatedProcessingCount)
            assertEquals(50, updatedWaitingCount)
        }

        @Test
        fun `PROCESSING 상태의 토큰이 이미 최대 개수일 때 스케줄러가 아무 작업도 수행하지 않아야 한다`() {
            // given
            createTokens(QueueManager.ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT)
            createTokens(5)

            // when
            queueScheduler.updateToProcessingTokens()

            // then
            val updatedProcessingCount = queueRedisRepository.getProcessingQueueCount()
            val updatedWaitingCount = queueRedisRepository.getWaitingQueueSize()

            assertEquals(QueueManager.ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT.toLong(), updatedProcessingCount)
            assertEquals(5, updatedWaitingCount)
        }

        @Test
        fun `WAITING 상태의 토큰이 없을 때 스케줄러가 아무 작업도 수행하지 않아야 한다`() {
            // given
            val processingCount = 3
            createTokens(processingCount)

            // when
            queueScheduler.updateToProcessingTokens()

            // then
            val updatedProcessingCount = queueRedisRepository.getProcessingQueueCount()
            val updatedWaitingCount = queueRedisRepository.getWaitingQueueSize()

            assertEquals(processingCount.toLong(), updatedProcessingCount)
            assertEquals(0, updatedWaitingCount)
        }

        private fun createTokens(count: Int) {
            repeat(count) {
                val userId = it
                queueManager.enqueueAndIssueToken(userId.toLong())
            }
        }
    }
}

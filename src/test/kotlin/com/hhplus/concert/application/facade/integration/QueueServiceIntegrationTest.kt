package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.QueueService
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.util.JwtUtil
import com.hhplus.concert.infrastructure.redis.QueueRedisRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
class QueueServiceIntegrationTest {
    @Autowired
    private lateinit var queueService: QueueService

    @Autowired
    private lateinit var queueRedisRepository: QueueRedisRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @BeforeEach
    fun setUp() {
        redisTemplate.execute { connection ->
            connection.flushAll()
        }
        userRepository.deleteAll()
    }

    @Nested
    @DisplayName("[대기열토큰발급] 테스트")
    inner class IssueQueueToken {
        @Test
        fun `유효한_사용자에_대해_새로운_토큰을_생성해야_한다`() {
            // given
            val user = userRepository.save(User(name = "TestUser"))

            // when
            val result = queueService.issueQueueToken(user.id)

            // then
            assertNotNull(result.token)
            assertTrue(jwtUtil.validateToken(result.token))
            assertEquals(user.id, jwtUtil.getUserIdFromToken(result.token))
        }

        @Test
        fun `대기중인_사용자가_다시_대기열에_시도할_경우_기존의_대기_중인_큐를_취소하고_새로운_큐를_생성해야_한다`() {
            // given
            val user = userRepository.save(User(name = "TestUser"))
            val oldToken = queueService.issueQueueToken(user.id).token

            // 기존 토큰이 대기열에 있는지 확인
            assertTrue(queueRedisRepository.getWaitingQueuePosition(oldToken, user.id.toString()) >= 0)

            Thread.sleep(1000)

            // when
            val newResult = queueService.issueQueueToken(user.id)

            // then
            assertNotEquals(oldToken, newResult.token)
            assertEquals(0, queueRedisRepository.getWaitingQueuePosition(oldToken, user.id.toString())) // 기존 토큰이 대기열에서 제거되었는지 확인
            assertTrue(queueRedisRepository.getWaitingQueuePosition(newResult.token, user.id.toString()) >= 0)
        }

        @Test
        fun `존재하지_않는_사용자_ID에_대해_예외를_발생시켜야_한다`() {
            // given
            val nonExistentUserId = 99999L

            // when & then
            assertThrows<BusinessException.NotFound> {
                queueService.issueQueueToken(nonExistentUserId)
            }
        }
    }

    @Nested
    @DisplayName("[대기열상태조회] 테스트")
    inner class FindQueueByTokenTest {
        @Test
        fun `대기_중인_큐에_대해_올바른_정보를_반환해야_한다`() {
            // given
            repeat(1500) { i ->
                val user = userRepository.save(User(name = "User$i"))
                queueRedisRepository.addToWaitingQueue("token_$i", user.id.toString(), System.currentTimeMillis())
            }
            val user = userRepository.save(User(name = "TestUser"))
            val token = queueService.issueQueueToken(user.id).token

            // when
            val result = queueService.findQueueByToken(token)

            // then
            assertEquals(QueueStatus.WAITING, result.status)
            assertTrue(result.remainingWaitListCount > 0)
            assertTrue(result.estimatedWaitTime > 0)
        }

        @Test
        fun `대기_중이_아닌_큐에_대해_remainingWaitListCount가_0이어야_한다`() {
            // given
            val user = userRepository.save(User(name = "TestUser"))
            val token = queueService.issueQueueToken(user.id).token
            queueRedisRepository.updateToProcessingQueue(token, user.id.toString(), System.currentTimeMillis() + 60000)

            val isInProcessingQueue = queueRedisRepository.isProcessingQueue(token)

            // when
            val result = queueService.findQueueByToken(token)

            // then
            assertTrue(isInProcessingQueue)
            assertEquals(QueueStatus.PROCESSING, result.status)
            assertEquals(0, result.remainingWaitListCount)
            assertEquals(0, result.estimatedWaitTime)
        }

        @Test
        fun `존재하지_않는_토큰으로_요청시_취소된 토큰으로 내려보내야 한다`() {
            // given
            val nonExistentToken = "non_existent_token"

            // when
            val result = queueService.findQueueByToken(nonExistentToken)

            // then
            assertEquals(QueueStatus.CANCELLED, result.status)
        }
    }

    @Test
    fun `처리중_대기열_업데이트_테스트`() {
        // given
        val users = List(3) { userRepository.save(User(name = "User$it")) }
        users.forEach { queueService.issueQueueToken(it.id) }

        // when
        queueService.updateToProcessingTokens()

        // then
        val processingCount = queueRedisRepository.getProcessingQueueCount()
        assertTrue(processingCount > 0)
        assertTrue(processingCount <= QueueManager.ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT)
    }

    @Test
    fun `만료된_대기열_취소_테스트`() {
        // given
        val user = userRepository.save(User(name = "TestUser"))
        val token = queueService.issueQueueToken(user.id).token

        // Redis에 만료된 항목 추가 (현재 시간보다 이전 시간으로 설정)
        val expiredTime = System.currentTimeMillis() - 1000000
        redisTemplate.opsForZSet().add(QueueRedisRepository.WAITING_QUEUE_KEY, "$token:${user.id}", expiredTime.toDouble())

        // 만료되지 않은 항목 추가
        val notExpiredTime = System.currentTimeMillis() + 1000000
        redisTemplate.opsForZSet().add(QueueRedisRepository.WAITING_QUEUE_KEY, "notExpired:${user.id}", notExpiredTime.toDouble())

        // when
        queueService.cancelExpiredWaitingQueue()

        // then
        val remainingCount = queueRedisRepository.getWaitingQueueSize()
        assertEquals(1, remainingCount) // 만료되지 않은 항목 1개만 남아있어야 함
    }
}

package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.QueueService
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.infrastructure.entity.Queue
import com.hhplus.concert.infrastructure.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Transactional
class QueueServiceIntegrationTest {
    @Autowired
    private lateinit var queueService: QueueService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var queueRepository: QueueRepository

    @Test
    fun `유효한 사용자에 대해 새로운 토큰을 생성해야 한다`() {
        // given
        val user = userRepository.save(User(name = "User"))

        // when
        val result = queueService.issueQueueToken(user.id)

        // then
        assertNotNull(result.token)
        assertNotNull(result.createdAt)

        val queue = queueRepository.findByUserIdAndStatus(user.id, QueueStatus.WAITING)
        assertNotNull(queue)
        assertEquals(result.token, queue!!.token)
    }

    @Nested
    @DisplayName("[issueQueueToken] 테스트")
    inner class IssueQueueToken {
        @Test
        fun `대기중인 사용자가 다시 대기열에 시도할 경우 기존의 대기 중인 큐를 취소하고 새로운 큐를 생성해야 한다`() {
            // given
            val user = userRepository.save(User(name = "User"))
            val existingQueue =
                queueRepository.save(
                    Queue(
                        user = user,
                        token = "old-token",
                        joinedAt = LocalDateTime.now().minusHours(1),
                        queueStatus = QueueStatus.WAITING,
                    ),
                )

            // when
            val result = queueService.issueQueueToken(user.id)

            // then
            assertNotNull(result.token)
            assertNotNull(result.createdAt)

            val cancelledQueue = queueRepository.findById(existingQueue!!.id)
            assertEquals(QueueStatus.CANCELLED, cancelledQueue!!.queueStatus)

            val newQueue = queueRepository.findByUserIdAndStatus(user.id, QueueStatus.WAITING)
            assertNotNull(newQueue)
            assertEquals(result.token, newQueue!!.token)
            assertNotEquals(existingQueue.token, newQueue.token)
        }

        @Test
        fun `존재하지 않는 사용자 ID에 대해 예외를 발생시켜야 한다`() {
            // given
            val nonExistentUserId = 99999L

            // when & then
            assertThrows<BusinessException.NotFound> {
                queueService.issueQueueToken(nonExistentUserId)
            }
        }

        @Test
        fun `다른 사용자의 큐에 영향을 주지 않아야 한다`() {
            // given
            val user1 = userRepository.save(User(name = "User 1"))
            val user2 = userRepository.save(User(name = "User 2"))
            queueRepository.save(
                Queue(
                    user = user2,
                    token = "user2-token",
                    joinedAt = LocalDateTime.now().minusHours(1),
                    queueStatus = QueueStatus.WAITING,
                ),
            )

            // when
            queueService.issueQueueToken(user1.id)

            // then
            val user1Queue = queueRepository.findByUserIdAndStatus(user1.id, QueueStatus.WAITING)
            assertNotNull(user1Queue)

            val user2Queue = queueRepository.findByUserIdAndStatus(user2.id, QueueStatus.WAITING)
            assertNotNull(user2Queue)
            assertEquals("user2-token", user2Queue!!.token)
        }
    }

    @Nested
    @DisplayName("[findQueueByToken] 테스트")
    inner class FindQueueByTokenTest {
        @Test
        fun `대기 중인 큐에 대해 올바른 정보를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))

            // 대기열에 이미 존재하는 WAITING 상태의 큐 생성
            repeat(5) {
                queueRepository.save(
                    Queue(
                        user = userRepository.save(User(name = "User $it")),
                        token = "token_$it",
                        joinedAt = LocalDateTime.now().minusMinutes(it.toLong()),
                        queueStatus = QueueStatus.WAITING,
                    ),
                )
            }

            val token = "test_token"
            val queue =
                queueRepository.save(
                    Queue(
                        user = user,
                        token = token,
                        joinedAt = LocalDateTime.now(),
                        queueStatus = QueueStatus.WAITING,
                    ),
                )

            // when
            val result = queueService.findQueueByToken(token)

            // then
            assertEquals(queue!!.id, result.queueId)
            assertEquals(QueueStatus.WAITING, result.status)
            assertEquals(queue.joinedAt, result.joinAt)
            assertEquals(5, result.remainingWaitListCount)
        }

        @Test
        fun `대기 중이 아닌 큐에 대해 remainingWaitListCount가 0이어야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = "test_token"
            val queue =
                queueRepository.save(
                    Queue(
                        user = user,
                        token = token,
                        joinedAt = LocalDateTime.now(),
                        queueStatus = QueueStatus.PROCESSING,
                    ),
                )

            // when
            val result = queueService.findQueueByToken(token)

            // then
            assertEquals(queue!!.id, result.queueId)
            assertEquals(QueueStatus.PROCESSING, result.status)
            assertEquals(queue.joinedAt, result.joinAt)
            assertEquals(0, result.remainingWaitListCount)
        }

        @Test
        fun `존재하지 않는 토큰으로 요청시 예외를 발생시켜야 한다`() {
            // given
            val nonExistentToken = "non_existent_token"

            // when & then
            assertThrows<BusinessException.NotFound> {
                queueService.findQueueByToken(nonExistentToken)
            }
        }
    }
}

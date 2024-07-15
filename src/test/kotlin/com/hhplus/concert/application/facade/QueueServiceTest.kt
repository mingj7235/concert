package com.hhplus.concert.application.facade

import com.hhplus.concert.business.application.service.QueueService
import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.business.domain.manager.user.UserManager
import com.hhplus.concert.common.error.code.UserErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.domain.manager.queue.QueueManagerTest
import com.hhplus.concert.infrastructure.entity.Queue
import com.hhplus.concert.infrastructure.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime

/**
 * QueueService 단위 테스트
 */
class QueueServiceTest {
    @Mock
    private lateinit var queueManager: QueueManager

    @Mock
    private lateinit var userManager: UserManager

    @InjectMocks
    private lateinit var queueService: QueueService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Nested
    @DisplayName("issueQueueToken 테스트")
    inner class IssueQueueTokenTest {
        @Test
        fun `기존 대기열이 없는 경우 새로운 토큰을 발급한다`() {
            // given
            val userId = 0L
            val user = User("User")

            `when`(userManager.findById(userId)).thenReturn(user)
            `when`(queueManager.findByUserIdAndStatus(userId, QueueStatus.WAITING)).thenReturn(null)
            `when`(queueManager.enqueueAndIssueToken(user)).thenReturn(TOKEN)

            // when
            val result = queueService.issueQueueToken(userId)

            // then
            assertEquals(TOKEN, result.token)
        }

        @Test
        fun `기존 대기열이 있는 경우 취소 후 새로운 토큰을 발급한다`() {
            // given
            val userId = 0L
            val user = User("User")
            val existingQueue =
                Queue(
                    user = user,
                    token = QueueManagerTest.TOKEN,
                    joinedAt = LocalDateTime.now(),
                    QueueStatus.WAITING,
                )

            `when`(userManager.findById(userId)).thenReturn(user)
            `when`(queueManager.findByUserIdAndStatus(userId, QueueStatus.WAITING)).thenReturn(existingQueue)
            `when`(queueManager.enqueueAndIssueToken(user)).thenReturn(TOKEN)

            // when
            val result = queueService.issueQueueToken(userId)

            // then
            assertEquals(TOKEN, result.token)
            verify(queueManager).updateStatus(existingQueue, QueueStatus.CANCELLED)
        }

        @Test
        fun `사용자가 존재하지 않는 경우 예외를 던진다`() {
            // given
            `when`(userManager.findById(NON_EXISTED_USER_ID)).thenThrow(BusinessException.NotFound(UserErrorCode.NOT_FOUND))

            // when & then
            assertThrows<BusinessException.NotFound> {
                queueService.issueQueueToken(NON_EXISTED_USER_ID)
            }
        }
    }

    @Nested
    @DisplayName("findQueueByToken 테스트")
    inner class FindQueueByTokenTest {
        @Test
        fun `token 을 통해 queue 를 조회하고, WAITING 상태라면 대기열이 얼마나 남았는지 계산하여 리턴한다`() {
            // Given
            val user = User("User")
            val queueId = 0L
            val queue =
                Queue(
                    user = user,
                    token = QueueManagerTest.TOKEN,
                    joinedAt = LocalDateTime.now(),
                    QueueStatus.WAITING,
                )
            val position = 5

            `when`(queueManager.findByToken(TOKEN)).thenReturn(queue)
            `when`(queueManager.getPositionInWaitingStatus(queueId)).thenReturn(position)

            // When
            val result = queueService.findQueueByToken(TOKEN)

            // Then
            assertEquals(queueId, result.queueId)
            assertEquals(QueueStatus.WAITING, result.status)
            assertEquals(position, result.remainingWaitListCount)
            verify(queueManager).findByToken(TOKEN)
            verify(queueManager).getPositionInWaitingStatus(queueId)
        }

        @Test
        fun `token 을 통해 queue 를 조회하고, WAITING 상태가 아니라면 대기열은 0을 리턴한다`() {
            // Given
            val user = User("User")
            val queueId = 0L
            val queue =
                Queue(
                    user = user,
                    token = QueueManagerTest.TOKEN,
                    joinedAt = LocalDateTime.now(),
                    QueueStatus.PROCESSING,
                )

            `when`(queueManager.findByToken(TOKEN)).thenReturn(queue)

            // When
            val result = queueService.findQueueByToken(TOKEN)

            // Then
            assertEquals(queueId, result.queueId)
            assertEquals(QueueStatus.PROCESSING, result.status)
            assertEquals(0, result.remainingWaitListCount)
            verify(queueManager).findByToken(TOKEN)
            verify(queueManager, never()).getPositionInWaitingStatus(queueId)
        }
    }

    @Nested
    @DisplayName("maintainProcessingCount 테스트")
    inner class MaintainProcessingCountTest {
        @Test
        fun `PROCESSING 상태인 큐 개수가 최대 허용 개수보다 적을 때 상태를 업데이트한다`() {
            // Given
            val currentProcessingCount = 95
            val neededToUpdateCount = ALLOWED_MAX_SIZE - currentProcessingCount

            val queueIdsToUpdate = listOf(1L, 2L, 3L, 4L, 5L)

            `when`(queueManager.countByQueueStatus(QueueStatus.PROCESSING)).thenReturn(currentProcessingCount)
            `when`(queueManager.getNeededUpdateToProcessingIdsFromWaiting(neededToUpdateCount)).thenReturn(queueIdsToUpdate)

            // When
            queueService.maintainProcessingCount()

            // Then
            verify(queueManager).countByQueueStatus(QueueStatus.PROCESSING)
            verify(queueManager).getNeededUpdateToProcessingIdsFromWaiting(neededToUpdateCount)
            verify(queueManager).updateStatus(queueIdsToUpdate, QueueStatus.PROCESSING)
        }

        @Test
        fun `처리 중인 큐 개수가 최대 허용 개수와 같을 때 상태를 업데이트하지 않는다`() {
            // Given
            val currentProcessingCount = ALLOWED_MAX_SIZE

            `when`(queueManager.countByQueueStatus(QueueStatus.PROCESSING)).thenReturn(currentProcessingCount)

            // When
            queueService.maintainProcessingCount()

            // Then
            verify(queueManager).countByQueueStatus(QueueStatus.PROCESSING)
            verify(queueManager, never()).getNeededUpdateToProcessingIdsFromWaiting(0)
            verify(queueManager, never()).updateStatus(emptyList(), QueueStatus.PROCESSING)
        }

        @Test
        fun `처리 중인 큐 개수가 최대 허용 개수를 초과할 때 상태를 업데이트하지 않는다`() {
            // Given
            val currentProcessingCount = ALLOWED_MAX_SIZE + 10

            `when`(queueManager.countByQueueStatus(QueueStatus.PROCESSING)).thenReturn(currentProcessingCount)

            // When
            queueService.maintainProcessingCount()

            // Then
            verify(queueManager).countByQueueStatus(QueueStatus.PROCESSING)
            verify(queueManager, never()).getNeededUpdateToProcessingIdsFromWaiting(0)
            verify(queueManager, never()).updateStatus(emptyList(), QueueStatus.PROCESSING)
        }
    }

    companion object {
        const val NON_EXISTED_USER_ID = -1L
        const val TOKEN = "test_token"
        const val ALLOWED_MAX_SIZE = 100
    }
}

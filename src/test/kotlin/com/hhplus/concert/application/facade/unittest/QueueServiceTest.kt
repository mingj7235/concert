package com.hhplus.concert.application.facade.unittest

import com.hhplus.concert.business.application.service.QueueService
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.manager.UserManager
import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.util.JwtUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * QueueService 단위 테스트
 */
class QueueServiceTest {
    @Mock
    private lateinit var queueManager: QueueManager

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var jwtUtil: JwtUtil

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
        fun `새로운 토큰을 발급한다`() {
            // given
            val userId = 1L
            val user = User("User")
            `when`(userManager.findById(userId)).thenReturn(user) // 사용자 존재 확인
            `when`(queueManager.enqueueAndIssueToken(userId)).thenReturn(TOKEN)

            // when
            val result = queueService.issueQueueToken(userId)

            // then
            assert(result.token == TOKEN)
            verify(queueManager).enqueueAndIssueToken(userId)
        }

        @Test
        fun `사용자가 존재하지 않는 경우 예외를 던진다`() {
            // given
            `when`(userManager.findById(NON_EXISTED_USER_ID)).thenThrow(BusinessException.NotFound(ErrorCode.User.NOT_FOUND))

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
        fun `token을 통해 queue를 조회하고, WAITING 상태라면 대기열 정보를 계산하여 리턴한다`() {
            // Given
            `when`(queueManager.getQueueStatus(TOKEN)).thenReturn(QueueStatus.WAITING)
            `when`(queueManager.getPositionInWaitingStatus(TOKEN)).thenReturn(5L)
            `when`(queueManager.calculateEstimatedWaitSeconds(5L)).thenReturn(300L)
            `when`(jwtUtil.getUserIdFromToken(TOKEN)).thenReturn(1L)

            // When
            val result = queueService.findQueueByToken(TOKEN)

            // Then
            assert(result.status == QueueStatus.WAITING)
            assert(result.remainingWaitListCount == 5L)
            assert(result.estimatedWaitTime == 300L)
        }

        @Test
        fun `token을 통해 queue를 조회하고, WAITING 상태가 아니라면 대기열은 0을 리턴한다`() {
            // Given
            `when`(queueManager.getQueueStatus(TOKEN)).thenReturn(QueueStatus.PROCESSING)
            `when`(jwtUtil.getUserIdFromToken(TOKEN)).thenReturn(1L)

            // When
            val result = queueService.findQueueByToken(TOKEN)

            // Then
            assert(result.status == QueueStatus.PROCESSING)
            assert(result.remainingWaitListCount == 0L)
            assert(result.estimatedWaitTime == 0L)
        }
    }

    @Nested
    @DisplayName("updateToProcessingTokens 테스트")
    inner class UpdateToProcessingTokensTest {
        @Test
        fun `PROCESSING 상태로 업데이트할 토큰이 있을 때 상태를 업데이트한다`() {
            // Given
            `when`(queueManager.updateToProcessingTokens())

            // When
            queueService.updateToProcessingTokens()

            // Then
            verify(queueManager).updateToProcessingTokens()
        }
    }

    @Nested
    @DisplayName("cancelExpiredWaitingQueue 테스트")
    inner class CancelExpiredWaitingQueueTest {
        @Test
        fun `만료된 대기열을 취소한다`() {
            // When
            queueService.cancelExpiredWaitingQueue()

            // Then
            verify(queueManager).removeExpiredWaitingQueue()
        }
    }

    companion object {
        const val NON_EXISTED_USER_ID = -1L
        const val TOKEN = "test_token"
    }
}

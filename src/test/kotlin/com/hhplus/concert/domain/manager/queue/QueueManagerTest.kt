package com.hhplus.concert.domain.manager.queue

import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.util.JwtUtil
import com.hhplus.concert.infrastructure.redis.QueueRedisRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * QueueManager 단위 테스트
 */
class QueueManagerTest {
    @Mock
    private lateinit var queueRedisRepository: QueueRedisRepository

    @Mock
    private lateinit var jwtUtil: JwtUtil

    @InjectMocks
    private lateinit var queueManager: QueueManager

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Nested
    @DisplayName("enqueueAndIssueToken 테스트")
    inner class EnqueueAndIssueTokenTest {
        @Test
        fun `존재하는 userId로 queue를 생성하고, token을 생성하여 리턴한다`() {
            // given
            val userId = 1L
            `when`(jwtUtil.generateToken(userId)).thenReturn(TOKEN)

            // when
            val result = queueManager.enqueueAndIssueToken(userId)

            // then
            assertThat(result).isEqualTo(TOKEN)
            verify(jwtUtil).generateToken(userId)
            verify(queueRedisRepository).addToWaitingQueue(TOKEN, userId.toString(), System.currentTimeMillis().toDouble())
        }
    }

    @Nested
    @DisplayName("getQueueStatus 테스트")
    inner class GetQueueStatusTest {
        @Test
        fun `PROCESSING 상태의 queue를 정확히 반환한다`() {
            // given
            `when`(queueRedisRepository.isProcessingQueue(TOKEN)).thenReturn(true)

            // when
            val result = queueManager.getQueueStatus(TOKEN)

            // then
            assertThat(result).isEqualTo(QueueStatus.PROCESSING)
        }

        @Test
        fun `WAITING 상태의 queue를 정확히 반환한다`() {
            // given
            val userId = 1L
            `when`(queueRedisRepository.isProcessingQueue(TOKEN)).thenReturn(false)
            `when`(queueRedisRepository.getWaitingQueuePosition(TOKEN, userId.toString())).thenReturn(1L)

            // when
            val result = queueManager.getQueueStatus(TOKEN)

            // then
            assertThat(result).isEqualTo(QueueStatus.WAITING)
        }

        @Test
        fun `CANCELLED 상태의 queue를 정확히 반환한다`() {
            // given
            val userId = 1L
            `when`(queueRedisRepository.isProcessingQueue(TOKEN)).thenReturn(false)
            `when`(queueRedisRepository.getWaitingQueuePosition(TOKEN, userId.toString())).thenReturn(0L)

            // when
            val result = queueManager.getQueueStatus(TOKEN)

            // then
            assertThat(result).isEqualTo(QueueStatus.CANCELLED)
        }
    }

    @Nested
    @DisplayName("getPositionInWaitingStatus 테스트")
    inner class GetPositionInWaitingStatusTest {
        @Test
        fun `대기 상태의 Queue 위치를 정확히 반환한다`() {
            // given
            val userId = 1L
            val expectedPosition = 3L
            `when`(queueRedisRepository.getWaitingQueuePosition(TOKEN, userId.toString())).thenReturn(expectedPosition)

            // when
            val result = queueManager.getPositionInWaitingStatus(TOKEN, userId.toString())

            // then
            assertThat(result).isEqualTo(expectedPosition)
        }
    }

    @Nested
    @DisplayName("updateToProcessingTokens 테스트")
    inner class UpdateToProcessingTokensTest {
        @Test
        fun `WAITING 상태에서 PROCESSING 상태로 변경될 토큰 목록을 정확히 반환한다`() {
            // given
            val availableProcessingRoom = 3
            val tokensToUpdate = listOf("token1" to "1", "token2" to "2", "token3" to "3")
            `when`(queueRedisRepository.getProcessingQueueCount()).thenReturn(
                (
                    QueueManager.ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT -
                        availableProcessingRoom
                ).toLong(),
            )
            `when`(queueRedisRepository.getWaitingQueueNeedToUpdateToProcessing(availableProcessingRoom)).thenReturn(tokensToUpdate)

            // when
            queueManager.updateToProcessingTokens()

            // then
            verify(queueRedisRepository).getProcessingQueueCount()
            verify(queueRedisRepository).getWaitingQueueNeedToUpdateToProcessing(availableProcessingRoom)
        }
    }

    @Nested
    @DisplayName("removeExpiredWaitingQueue 테스트")
    inner class RemoveExpiredWaitingQueueTest {
        @Test
        fun `만료된 대기 Queue를 제거한다`() {
            // when
            queueManager.removeExpiredWaitingQueue()

            // then
            verify(queueRedisRepository).removeExpiredWaitingQueue(System.currentTimeMillis())
        }
    }

    @Nested
    @DisplayName("completeProcessingToken 테스트")
    inner class CompleteProcessingTokenTest {
        @Test
        fun `처리 완료된 토큰을 제거한다`() {
            // when
            queueManager.completeProcessingToken(TOKEN)

            // then
            verify(queueRedisRepository).removeProcessingToken(TOKEN)
        }
    }

    @Nested
    @DisplayName("calculateEstimatedWaitSeconds 테스트")
    inner class CalculateEstimatedWaitSecondsTest {
        @Test
        fun `대기 시간을 정확히 계산한다`() {
            // given
            val position = 2500L
            val expectedWaitTime = 750L // (2500 / 1000) * (60 * 5)

            // when
            val result = queueManager.calculateEstimatedWaitSeconds(position)

            // then
            assertThat(result).isEqualTo(expectedWaitTime)
        }
    }

    companion object {
        const val TOKEN = "test_token"
    }
}

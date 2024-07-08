package com.hhplus.concert.domain.manager.queue

import com.hhplus.concert.common.exception.error.QueueException
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.util.JwtUtil
import com.hhplus.concert.domain.repository.QueueRepository
import com.hhplus.concert.infra.entity.Queue
import com.hhplus.concert.infra.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.time.LocalDateTime

/**
 * QueueManager 단위 테스트
 */
class QueueManagerTest {
    @Mock
    private lateinit var queueRepository: QueueRepository

    @Mock
    private lateinit var jwtUtil: JwtUtil

    @InjectMocks
    private lateinit var queueManager: QueueManager

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @AfterEach
    fun deleteAll() {
        queueRepository.deleteAll()
    }

    @Nested
    @DisplayName("findByUserIdAndStatus 테스트")
    inner class FindByUserIdAndStatusTest {
        @Test
        fun `존재하지 않는 userId 로 queue 를 조회한다면 null 을 리턴한다`() {
            `when`(
                queueRepository.findByUserIdAndStatus(
                    userId = NON_EXISTED_USER_ID,
                    queueStatus = QueueStatus.WAITING,
                ),
            ).thenReturn(null)

            val result =
                queueManager.findByUserIdAndStatus(
                    userId = NON_EXISTED_USER_ID,
                    queueStatus = QueueStatus.WAITING,
                )

            assertThat(result).isNull()
        }

        @Test
        fun `존재하는 userId 와 queueStatus 로 queue 를 조회하면 올바른 queue 를 리턴한다`() {
            val userId = 0L
            val user = User("User")
            val queue =
                Queue(
                    user = user,
                    token = TOKEN,
                    joinedAt = LocalDateTime.now(),
                    QueueStatus.WAITING,
                )

            `when`(
                queueRepository.findByUserIdAndStatus(
                    userId = userId,
                    queueStatus = QueueStatus.WAITING,
                ),
            ).thenReturn(queue)

            val result =
                queueManager.findByUserIdAndStatus(
                    userId = userId,
                    queueStatus = QueueStatus.WAITING,
                )

            assertThat(result).isNotNull
            assertThat(result!!.user.id).isEqualTo(userId)
            assertThat(result.token).isEqualTo(TOKEN)
            assertThat(result.queueStatus).isEqualTo(QueueStatus.WAITING)
        }
    }

    @Nested
    @DisplayName("enqueueAndIssueToken 테스트")
    inner class EnqueueAndIssueTokenTest {
        @Test
        fun `존재하는 user 로 queue 를 생성하고, token 을 생성하여 리턴한다`() {
            // given
            val userId = 0L
            val user = User("User")

            `when`(jwtUtil.generateToken(userId)).thenReturn(TOKEN)

            // when
            val result =
                queueManager.enqueueAndIssueToken(user)

            // then
            assertThat(result).isEqualTo(TOKEN)
            verify(jwtUtil).generateToken(userId)
        }
    }

    @Nested
    @DisplayName("updateStatus 테스트 (단일 queue)")
    inner class UpdateStatusTest {
        @Test
        fun `queue 의 상태를 정상적으로 업데이트한다 (waiting 에서 processing 으로)`() {
            // given
            val user = User("User")
            val queue =
                Queue(
                    user = user,
                    token = TOKEN,
                    joinedAt = LocalDateTime.now(),
                    QueueStatus.WAITING,
                )

            // when
            queueManager.updateStatus(queue, QueueStatus.PROCESSING)

            // then
            assertThat(queue.queueStatus).isEqualTo(QueueStatus.PROCESSING)
            verify(queueRepository).save(queue)
        }
    }

    @Nested
    @DisplayName("updateStatus 테스트 (복수의 queue)")
    inner class UpdateStatusTest2 {
        @Test
        fun `여러 Queue의 상태를 한 번에 업데이트한다`() {
            // given
            val queueIds = listOf(1L, 2L, 3L)
            val newStatus = QueueStatus.PROCESSING

            // when
            queueManager.updateStatus(queueIds, newStatus)

            // then
            verify(queueRepository).updateStatusForIds(queueIds, newStatus)
        }

        @Test
        fun `빈 리스트로 호출 시 InvalidRequest 예외를 던진다`() {
            // given
            val emptyQueueIds = emptyList<Long>()
            val newStatus = QueueStatus.COMPLETED

            // when & then
            assertThrows<QueueException.InvalidRequest> {
                queueManager.updateStatus(emptyQueueIds, newStatus)
            }
        }

        @Test
        fun `단일 ID로 호출 시 정상적으로 처리된다`() {
            // given
            val singleQueueId = listOf(1L)
            val newStatus = QueueStatus.PROCESSING

            // when
            queueManager.updateStatus(singleQueueId, newStatus)

            // then
            verify(queueRepository).updateStatusForIds(singleQueueId, newStatus)
        }
    }

    @Nested
    @DisplayName("findByToken 테스트")
    inner class FindByTokenTest {
        @Test
        fun `존재하는 토큰으로 Queue를 찾는다`() {
            // given
            val user = User("User")
            val expectedQueue =
                Queue(
                    user = user,
                    token = TOKEN,
                    joinedAt = LocalDateTime.now(),
                    QueueStatus.WAITING,
                )

            `when`(queueRepository.findByToken(TOKEN)).thenReturn(expectedQueue)

            // when
            val result = queueManager.findByToken(TOKEN)

            // then
            assertEquals(expectedQueue, result)
        }

        @Test
        fun `존재하지 않는 토큰으로 조회 시 예외를 던진다`() {
            // given
            val token = "invalidToken"
            `when`(queueRepository.findByToken(token)).thenReturn(null)

            // when & then
            assertThrows<QueueException.QueueNotFound> {
                queueManager.findByToken(token)
            }
        }
    }

    @Nested
    @DisplayName("getPositionInWaitingStatus 테스트")
    inner class GetPositionInWaitingStatusTest {
        @Test
        fun `대기 상태의 Queue 위치를 정확히 반환한다`() {
            // given
            val queueId = 1L
            val expectedPosition = 3
            `when`(queueRepository.getPositionInStatus(queueId, QueueStatus.WAITING)).thenReturn(expectedPosition)

            // when
            val result = queueManager.getPositionInWaitingStatus(queueId)

            // then
            assertEquals(expectedPosition, result)
        }
    }

    @Nested
    @DisplayName("countByQueueStatus 테스트")
    inner class CountByQueueStatusTest {
        @Test
        fun `특정 상태의 Queue 개수를 정확히 반환한다`() {
            // given
            val status = QueueStatus.WAITING
            val expectedCount = 5
            `when`(queueRepository.countByQueueStatus(status)).thenReturn(expectedCount)

            // when
            val result = queueManager.countByQueueStatus(status)

            // then
            assertEquals(expectedCount, result)
        }
    }

    @Nested
    @DisplayName("getNeededUpdateToProcessingIdsFromWaiting 테스트")
    inner class GetNeededUpdateToProcessingIdsFromWaitingTest {
        @Test
        fun `대기 상태에서 처리 상태로 변경될 Queue ID 목록을 정확히 반환한다`() {
            // given
            val neededToUpdateCount = 3
            val mockQueues =
                (1..3).map { i ->
                    Queue(
                        user = User("User$i"),
                        token = "TOKEN$i",
                        joinedAt = LocalDateTime.now(),
                        queueStatus = QueueStatus.WAITING,
                    ).apply {
                        val field = Queue::class.java.getDeclaredField("id")
                        field.isAccessible = true
                        field.set(this, i.toLong())
                    }
                }

            val expectedIds = mockQueues.map { it.id }

            `when`(queueRepository.findTopByStatusOrderByIdAsc(QueueStatus.WAITING, neededToUpdateCount))
                .thenReturn(mockQueues)

            // when
            val result = queueManager.getNeededUpdateToProcessingIdsFromWaiting(neededToUpdateCount)

            // then
            assertEquals(expectedIds, result)
        }

        @Test
        fun `요청한 수보다 적은 대기 Queue가 있을 경우 가능한 만큼만 반환한다`() {
            // given
            val neededToUpdateCount = 5
            val mockQueues =
                (1..3).map { i ->
                    Queue(
                        user = User("User$i"),
                        token = "TOKEN$i",
                        joinedAt = LocalDateTime.now(),
                        queueStatus = QueueStatus.WAITING,
                    ).apply {
                        val field = Queue::class.java.getDeclaredField("id")
                        field.isAccessible = true
                        field.set(this, i.toLong())
                    }
                }

            val availableIds = mockQueues.map { it.id }

            `when`(queueRepository.findTopByStatusOrderByIdAsc(QueueStatus.WAITING, neededToUpdateCount))
                .thenReturn(mockQueues)

            // when
            val result = queueManager.getNeededUpdateToProcessingIdsFromWaiting(neededToUpdateCount)

            // then
            assertEquals(availableIds, result)
        }
    }

    companion object {
        const val NON_EXISTED_USER_ID = -1L
        const val TOKEN = "test_token"
    }
}

package com.hhplus.concert.interfaces.presentation.controller

import com.hhplus.concert.business.domain.entity.Queue
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.constants.TokenConstants.QUEUE_TOKEN_HEADER
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.util.JwtUtil
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QueueControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var queueRepository: QueueRepository

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Nested
    @DisplayName("[issueQueueToken] 테스트")
    inner class IssueQueueTokenTest {
        @Test
        fun `유효한 사용자에 대해 새로운 토큰을 생성해야 한다`() {
            // given
            val user = userRepository.save(User(name = "User"))

            // when
            val result =
                mockMvc.post("/api/v1/queue/users/${user.id}") {
                    contentType = MediaType.APPLICATION_JSON
                }

            // then
            result.andExpect {
                status { isOk() }
                jsonPath("$.token").exists()
                jsonPath("$.createdAt").exists()
            }

            val queue = queueRepository.findByUserIdAndStatus(user.id, QueueStatus.WAITING)
            assert(queue != null)
        }

        @Test
        fun `대기중인 사용자가 다시 대기열에 시도할 경우 기존의 대기 중인 큐를 취소하고 새로운 큐를 생성해야 한다`() {
            // given
            val user = userRepository.save(User(name = "User"))

            // when
            val existingQueue =
                queueRepository.save(
                    Queue(
                        user = user,
                        token = "old-token",
                        joinedAt = LocalDateTime.now().minusHours(1),
                        queueStatus = QueueStatus.WAITING,
                    ),
                )

            val result =
                mockMvc.post("/api/v1/queue/users/${user.id}") {
                    contentType = MediaType.APPLICATION_JSON
                }

            // then
            result.andExpect {
                status { isOk() }
                jsonPath("$.token").exists()
                jsonPath("$.createdAt").exists()
            }

            val cancelledQueue = queueRepository.findById(existingQueue!!.id)
            assert(cancelledQueue!!.queueStatus == QueueStatus.CANCELLED)

            val newQueue = queueRepository.findByUserIdAndStatus(user.id, QueueStatus.WAITING)
            assert(newQueue != null)
            assert(newQueue!!.token != existingQueue.token)
        }

        @Test
        fun `존재하지 않는 사용자 ID에 대해 에러를 반환해야 한다`() {
            // given
            val nonExistentUserId = 99999L

            // when & then
            mockMvc
                .post("/api/v1/queue/users/$nonExistentUserId") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isNotFound() }
                }
        }

        @Test
        fun `잘못된 형식의 사용자 ID를 처리해야 한다`() {
            mockMvc
                .post("/api/v1/queue/users/invalid-id") {
                    contentType = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isBadRequest() }
                }
        }
    }

    @Nested
    @DisplayName("[getQueueStatus] 테스트")
    inner class GetQueueStatusTest {
        @Test
        fun `유효한 토큰으로 대기 중인 큐 상태를 조회해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            val result =
                mockMvc.get("/api/v1/queue/users") {
                    header(QUEUE_TOKEN_HEADER, token)
                }

            // then
            result.andExpect {
                status { isOk() }
                jsonPath("$.queueId").value(queue!!.id)
                jsonPath("$.status").value(QueueStatus.WAITING.name)
                jsonPath("$.joinAt").exists()
                jsonPath("$.remainingWaitListCount").isNumber
            }
        }

        @Test
        fun `유효하지 않은 토큰으로 요청시 인증 에러를 반환해야 한다`() {
            // given
            val invalidToken = "invalid_token"

            // when & then
            mockMvc
                .get("/api/v1/queue/users") {
                    header(QUEUE_TOKEN_HEADER, invalidToken)
                }.andExpect {
                    status { isUnauthorized() }
                }
        }

        @Test
        fun `토큰이 없는 요청에 대해 인증 에러를 반환해야 한다`() {
            // when & then
            mockMvc.get("/api/v1/queue/users").andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `존재하지 않는 큐에 대한 요청시 Not Found 에러를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)

            // when & then
            mockMvc
                .get("/api/v1/queue/users") {
                    header(QUEUE_TOKEN_HEADER, token)
                }.andExpect {
                    status { isNotFound() }
                }
        }
    }
}

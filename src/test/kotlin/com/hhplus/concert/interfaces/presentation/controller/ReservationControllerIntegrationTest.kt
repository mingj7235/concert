package com.hhplus.concert.interfaces.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.constants.TokenConstants.QUEUE_TOKEN_HEADER
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.common.util.JwtUtil
import com.hhplus.concert.infrastructure.redis.QueueRedisRepository
import com.hhplus.concert.interfaces.presentation.request.ReservationRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var queueRedisRepository: QueueRedisRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var concertScheduleRepository: ConcertScheduleRepository

    @Autowired
    private lateinit var seatRepository: SeatRepository

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Test
    fun `유효한 토큰과 요청으로 좌석 예약에 성공해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = jwtUtil.generateToken(user.id)
        queueRedisRepository.addToWaitingQueue(
            token,
            user.id.toString(),
            System.currentTimeMillis() + 1000 * 60 * 5, // 5분
        )

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        val schedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )
        val seat =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.AVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )

        val reservationRequest =
            ReservationRequest.Detail(
                userId = user.id,
                concertId = concert.id,
                scheduleId = schedule.id,
                seatIds = listOf(seat.id),
            )

        // when
        val result =
            mockMvc.post("/api/v1/reservations") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(reservationRequest)
            }

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$[0].reservationId").exists()
            jsonPath("$[0].concertId").value(concert.id)
            jsonPath("$[0].concertName").value(concert.title)
            jsonPath("$[0].concertAt").exists()
            jsonPath("$[0].seat.seatNumber").value(seat.seatNumber)
            jsonPath("$[0].seat.price").value(seat.seatPrice)
            jsonPath("$[0].reservationStatus").value(ReservationStatus.PAYMENT_PENDING.name)
        }
    }

    @Test
    fun `유효하지 않은 토큰으로 요청시 인증 에러를 반환해야 한다`() {
        // given
        val invalidToken = "invalid_token"
        val reservationRequest =
            ReservationRequest.Detail(
                userId = 1L,
                concertId = 1L,
                scheduleId = 1L,
                seatIds = listOf(1L),
            )

        // when & then
        mockMvc
            .post("/api/v1/reservations") {
                header(QUEUE_TOKEN_HEADER, invalidToken)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(reservationRequest)
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `PROCESSING 상태가 아닌 큐를 가진 사용자의 요청에 대해 에러를 반환해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = jwtUtil.generateToken(user.id)
        queueRedisRepository.addToWaitingQueue(
            token,
            user.id.toString(),
            System.currentTimeMillis() + 1000 * 60 * 5, // 5분
        )

        val reservationRequest =
            ReservationRequest.Detail(
                userId = user.id,
                concertId = 1L,
                scheduleId = 1L,
                seatIds = listOf(1L),
            )

        // when & then
        mockMvc
            .post("/api/v1/reservations") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(reservationRequest)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `존재하지 않는 사용자 ID로 요청시 Not Found 에러를 반환해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = jwtUtil.generateToken(user.id)
        queueRedisRepository.addToWaitingQueue(
            token,
            user.id.toString(),
            System.currentTimeMillis() + 1000 * 60 * 5, // 5분
        )

        val nonExistentUserId = 99999L
        val reservationRequest =
            ReservationRequest.Detail(
                userId = nonExistentUserId,
                concertId = 1L,
                scheduleId = 1L,
                seatIds = listOf(1L),
            )

        // when & then
        mockMvc
            .post("/api/v1/reservations") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(reservationRequest)
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `존재하지 않는 콘서트 ID로 요청시 Not Found 에러를 반환해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = jwtUtil.generateToken(user.id)
        queueRedisRepository.addToWaitingQueue(
            token,
            user.id.toString(),
            System.currentTimeMillis() + 1000 * 60 * 5, // 5분
        )

        val nonExistentConcertId = 99999L
        val reservationRequest =
            ReservationRequest.Detail(
                userId = user.id,
                concertId = nonExistentConcertId,
                scheduleId = 1L,
                seatIds = listOf(1L),
            )

        // when & then
        mockMvc
            .post("/api/v1/reservations") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(reservationRequest)
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `존재하지 않는 스케줄 ID로 요청시 Not Found 에러를 반환해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = jwtUtil.generateToken(user.id)
        queueRedisRepository.addToWaitingQueue(
            token,
            user.id.toString(),
            System.currentTimeMillis() + 1000 * 60 * 5, // 5분
        )

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        val nonExistentScheduleId = 99999L
        val reservationRequest =
            ReservationRequest.Detail(
                userId = user.id,
                concertId = concert.id,
                scheduleId = nonExistentScheduleId,
                seatIds = listOf(1L),
            )

        // when & then
        mockMvc
            .post("/api/v1/reservations") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(reservationRequest)
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `이미 예약된 좌석을 요청시 Bad Request 에러를 반환해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = jwtUtil.generateToken(user.id)
        queueRedisRepository.addToWaitingQueue(
            token,
            user.id.toString(),
            System.currentTimeMillis() + 1000 * 60 * 5, // 5분
        )

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        val schedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )
        val seat =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.UNAVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )

        val reservationRequest =
            ReservationRequest.Detail(
                userId = user.id,
                concertId = concert.id,
                scheduleId = schedule.id,
                seatIds = listOf(seat.id),
            )

        // when & then
        mockMvc
            .post("/api/v1/reservations") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(reservationRequest)
            }.andExpect {
                status { isBadRequest() }
            }
    }
}

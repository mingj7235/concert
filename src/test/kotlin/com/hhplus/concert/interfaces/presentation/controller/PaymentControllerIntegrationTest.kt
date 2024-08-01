package com.hhplus.concert.interfaces.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.PaymentHistoryRepository
import com.hhplus.concert.business.domain.repository.PaymentRepository
import com.hhplus.concert.business.domain.repository.ReservationRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.constants.TokenConstants.QUEUE_TOKEN_HEADER
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.PaymentStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.common.util.JwtUtil
import com.hhplus.concert.infrastructure.redis.QueueRedisRepository
import com.hhplus.concert.interfaces.presentation.request.PaymentRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
class PaymentControllerIntegrationTest {
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
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var paymentHistoryRepository: PaymentHistoryRepository

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Test
    fun `유효한 요청으로 결제를 성공적으로 처리해야 한다`() {
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

        val reservation =
            reservationRepository.save(
                Reservation(
                    user = user,
                    concertTitle = concert.title,
                    concertAt = schedule.concertAt,
                    seat = seat,
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now(),
                ),
            )

        val paymentRequest = PaymentRequest.Detail(reservationIds = listOf(reservation.id))

        // when
        val result =
            mockMvc.post("/api/v1/payment/payments/users/${user.id}") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(paymentRequest)
            }

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$[0].paymentId").exists()
            jsonPath("$[0].amount").value(10000)
            jsonPath("$[0].paymentStatus").value("COMPLETED")
        }

        // 추가 검증
        val updatedReservation = reservationRepository.findById(reservation.id)
        assertEquals(ReservationStatus.PAYMENT_COMPLETED, updatedReservation!!.reservationStatus)

        val payment = paymentRepository.findByReservationId(reservation.id)
        assertNotNull(payment)
        assertEquals(PaymentStatus.COMPLETED, payment?.paymentStatus)

        val paymentHistories = paymentHistoryRepository.findAllByPaymentId(payment?.id!!)
        assertNotNull(paymentHistories)
        assertEquals(1, paymentHistories.size)
    }

    @Test
    fun `유효하지 않은 토큰으로 요청시 인증 에러를 반환해야 한다`() {
        // given
        val invalidToken = "invalid_token"
        val userId = 1L
        val paymentRequest = PaymentRequest.Detail(reservationIds = listOf(1L))

        // when & then
        mockMvc
            .post("/api/v1/payment/payments/users/$userId") {
                header(QUEUE_TOKEN_HEADER, invalidToken)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(paymentRequest)
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `요청한 사용자와 예약의 사용자가 일치하지 않을 경우 에러를 반환해야 한다`() {
        // given
        val user1 = userRepository.save(User(name = "User 1"))
        val user2 = userRepository.save(User(name = "User 2"))
        val token = jwtUtil.generateToken(user1.id)
        queueRedisRepository.addToWaitingQueue(
            token,
            user1.id.toString(),
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

        val reservation =
            reservationRepository.save(
                Reservation(
                    user = user2, // 다른 사용자의 예약
                    concertTitle = concert.title,
                    concertAt = schedule.concertAt,
                    seat = seat,
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,
                    createdAt = LocalDateTime.now(),
                ),
            )

        val paymentRequest = PaymentRequest.Detail(reservationIds = listOf(reservation.id))

        // when & then
        mockMvc
            .post("/api/v1/payment/payments/users/${user1.id}") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(paymentRequest)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `존재하지 않는 예약 ID로 요청시 에러를 반환해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = jwtUtil.generateToken(user.id)
        queueRedisRepository.addToWaitingQueue(
            token,
            user.id.toString(),
            System.currentTimeMillis() + 1000 * 60 * 5, // 5분
        )

        val nonExistentReservationId = 99999L
        val paymentRequest = PaymentRequest.Detail(reservationIds = listOf(nonExistentReservationId))

        // when & then
        mockMvc
            .post("/api/v1/payment/payments/users/${user.id}") {
                header(QUEUE_TOKEN_HEADER, token)
                contentType = MediaType.APPLICATION_JSON
                content = ObjectMapper().writeValueAsString(paymentRequest)
            }.andExpect {
                status { isNotFound() }
            }
    }
}

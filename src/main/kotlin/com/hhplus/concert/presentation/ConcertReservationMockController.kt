package com.hhplus.concert.presentation

import com.hhplus.concert.common.annotation.TokenRequired
import com.hhplus.concert.common.type.PaymentStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.presentation.request.BalanceRequest
import com.hhplus.concert.presentation.request.PaymentRequest
import com.hhplus.concert.presentation.request.ReservationRequest
import com.hhplus.concert.presentation.response.BalanceResponse
import com.hhplus.concert.presentation.response.ConcertResponse
import com.hhplus.concert.presentation.response.PaymentResponse
import com.hhplus.concert.presentation.response.QueueTokenResponse
import com.hhplus.concert.presentation.response.ReservationResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Mock API 를 모아놓은 Controller
 * - 실제 API 는 각 Controller 로 분리한다.
 * - @TokenRequired 를 통해 header 에 담긴 TOKEN-ID 는 TokenInterceptor 에서 검증한다.
 */
@RestController
@RequestMapping("/mock")
class ConcertReservationMockController {
    // 대기열에 저장을 하고, 대기열 토큰을 발급한다.
    @PostMapping("/queue-tokens/users/{userId}")
    fun issueQueueToken(
        @PathVariable userId: Long,
    ): QueueTokenResponse.Token =
        QueueTokenResponse.Token(
            tokenId = 1L,
            createdAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now().plusMinutes(10),
        )

    // token 정보를 조회한다.
    @GetMapping("/queue-tokens/users/{userId}")
    fun getQueueToken(
        @PathVariable userId: Long,
    ): QueueTokenResponse.Token =
        QueueTokenResponse.Token(
            tokenId = 1L,
            createdAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now().plusMinutes(10),
        )

    // 대기열 정보를 조회한다.
    @TokenRequired
    @GetMapping("/queue-status/users/{userId}")
    fun getQueueStatus(
        @PathVariable userId: Long,
    ): QueueTokenResponse.Queue =
        QueueTokenResponse.Queue(
            queueId = 1L,
            joinAt = LocalDateTime.now(),
            status = QueueStatus.WAITING,
            remainingWaitListCount = 10,
        )

    // 콘서트 예약 가능 날짜 목록을 조회한다.
    @TokenRequired
    @GetMapping("/concerts/{concertId}/schedules")
    fun getConcertSchedules(
        @PathVariable concertId: Long,
    ): ConcertResponse.Schedule =
        ConcertResponse.Schedule(
            concertId = 1L,
            events =
                listOf(
                    ConcertResponse.Event(
                        scheduleId = 1L,
                        concertAt = LocalDateTime.now().plusMonths(1),
                        reservationAt = LocalDateTime.now().plusDays(15),
                    ),
                    ConcertResponse.Event(
                        scheduleId = 2L,
                        concertAt = LocalDateTime.now().plusMonths(2),
                        reservationAt = LocalDateTime.now().plusDays(20),
                    ),
                ),
        )

    // 콘서트 해당 날짜의 좌석을 조회한다.
    @TokenRequired
    @GetMapping("/concerts/{concertId}/schedules/{scheduleId}/seats")
    fun getSeats(
        @PathVariable concertId: Long,
        @PathVariable scheduleId: Long,
    ): ConcertResponse.AvailableSeat =
        ConcertResponse.AvailableSeat(
            concertId = 1L,
            concertAt = LocalDateTime.now().plusMonths(1),
            seats =
                listOf(
                    ConcertResponse.Seat(
                        seatId = 1L,
                        seatNumber = 20,
                        seatStatus = SeatStatus.AVAILABLE,
                        seatPrice = 10000,
                    ),
                    ConcertResponse.Seat(
                        seatId = 2L,
                        seatNumber = 21,
                        seatStatus = SeatStatus.UNAVAILABLE,
                        seatPrice = 10000,
                    ),
                ),
        )

    // 좌석을 예약한다.
    @TokenRequired
    @PostMapping("/reservations")
    fun createReservation(
        @RequestBody reservationRequest: ReservationRequest.Detail,
    ): ReservationResponse.Result =
        ReservationResponse.Result(
            reservationId = 1L,
            concertId = 1L,
            concertName = "콘서트",
            concertAt = LocalDateTime.now().plusMonths(1),
            seats =
                listOf(
                    ReservationResponse.Seat(
                        seatNumber = 10,
                        price = 10000,
                    ),
                    ReservationResponse.Seat(
                        seatNumber = 11,
                        price = 15000,
                    ),
                ),
            totalPrice = 25000,
            reservationStatus = ReservationStatus.PAYMENT_PENDING,
        )

    // 결제를 진행한다.
    @TokenRequired
    @PostMapping("/payments/users/{userId}")
    fun executePayment(
        @PathVariable userId: Long,
        @RequestBody paymentRequest: PaymentRequest.Detail,
    ): PaymentResponse.Result =
        PaymentResponse.Result(
            paymentId = 1L,
            amount = 30000,
            paymentStatus = PaymentStatus.COMPLETED,
        )

    // 잔액을 충전한다.
    @PostMapping("/balance/users/{userId}/recharge")
    fun addBalanceTransaction(
        @PathVariable userId: Long,
        @RequestBody rechargeRequest: BalanceRequest.Recharge,
    ): BalanceResponse.Detail =
        BalanceResponse.Detail(
            userId = 1L,
            currentAmount = 40000,
        )

    // 잔액을 조회한다.
    @GetMapping("/balance/users/{userId}")
    fun getUserBalance(
        @PathVariable userId: Long,
    ): BalanceResponse.Detail =
        BalanceResponse.Detail(
            userId = 1L,
            currentAmount = 40000,
        )
}
